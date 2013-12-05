/*!
* Copyright 2002 - 2013 Webdetails, a Pentaho company. All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cpf;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Callable;

import java.lang.reflect.Method;
import javax.ws.rs.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.web.http.request.HttpRequestParameterProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import pt.webdetails.cpf.plugincall.api.IPluginCall;
import pt.webdetails.cpf.web.CpfHttpServletRequest;
import pt.webdetails.cpf.web.CpfHttpServletResponse;


/**
 * Call to another pentaho plugin getting the bean from the plugin bean factory.
 * Not thread safe. - really ? Why not ?
 */
public class InterPluginCall implements Runnable, Callable<String>, IPluginCall {

  public final static Plugin CDA = new Plugin("cda");
  public final static Plugin CDB = new Plugin("cdb");
  public final static Plugin CDC = new Plugin("cdc");
  public final static Plugin CDE = new Plugin("pentaho-cdf-dd");
  public final static Plugin CDF = new Plugin("pentaho-cdf");
  public final static Plugin CDV = new Plugin("cdv");
  private Object objectResponse;


  @Override
  public String call(Map<String, String[]> params) throws Exception {
    Iterator<String> it = params.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      requestParameters.put(key, params.get(key));
    }
    return call();
  }

  @Override
  public void run(Map<String, String[]> params) throws Exception {
    Iterator<String> it = params.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      requestParameters.put(key, params.get(key));
    }

    run();
  }

  @Override
  public InputStream getResult() {
    return null;  //REVIEW
  }

  @Override
  public boolean exists() {
    return getPluginManager().getClassLoader(plugin.getName()) != null;
  }

  public static class Plugin {

    private String name;
    private String title;

    public String getName() {
      return name;
    }

    public String getTitle() {
      return title;
    }

    public Plugin(String name, String title) {
      this.name = name;
      this.title = title;
    }

    public Plugin(String id) {
      this.name = id;
      this.title = id;
    }

  }

  private static final Log logger = LogFactory.getLog(InterPluginCall.class);

  private Plugin plugin;
  private String method;
  private String service;


  private Map<String, Object> requestParameters;
  private HttpServletResponse response;
  private HttpServletRequest request;

  private IPentahoSession session;
  private IPluginManager pluginManager;


  public InterPluginCall(Plugin plugin, String method) {
    this(plugin, plugin.getName() + ".api", method);
  }


  public InterPluginCall(Plugin plugin, String method, Map<String, Object> params) {
    this(plugin, plugin.getName() + ".api", method, params);
  }

  public InterPluginCall(Plugin plugin, String service, String method) {

    if (plugin == null) throw new IllegalArgumentException("Plugin must be specified");

    this.plugin = plugin;
    this.method = method;
    this.service = service != null ? service : plugin.getName() + ".api";
    this.requestParameters = new HashMap<String, Object>();
  }

  public InterPluginCall(Plugin plugin, String service, String method, Map<String, Object> params) {
    this(plugin, service, method);


    this.requestParameters.putAll(params != null ? params : new HashMap<String, Object>());
  }



  protected HttpServletRequest getRequest() {
    if (request == null) {
      request = new pt.webdetails.cpf.web.CpfHttpServletRequest();
    }
    return request;
  }


  public InterPluginCall putParameter(String name, Object value) {
    requestParameters.put(name, value);
    return this;
  }


  private Object getBeanObject() {
    ListableBeanFactory beanFactory = getPluginManager().getBeanFactory(plugin.getName());

    if (beanFactory == null) {
      if (pluginManager.getClassLoader(plugin.getName()) == null) {
        logger.error("No such plugin: " + plugin.getName());
      } else {
        logger.error("No bean factory for plugin: " + plugin.getName());
      }
      return null;
    }

    if (!beanFactory.containsBean(service)) {
      logger.error("'" + service + "' bean not found in " + plugin.getName());
      return null;
    }

    return beanFactory.getBean(service);
  }

  public void run() {

    Class<?> classe = null;
    Method operation = null;
    Object o = null;
    //  try {
    o = getBeanObject();
    classe = o.getClass();
    Method[] methods = classe.getMethods();


    for (Method m : methods) {
      if (m.getName().equals( method )) {
        operation = m;
        break;
      }
    }

    Annotation[][] params = operation.getParameterAnnotations();
    Class<?>[] paramTypes = operation.getParameterTypes();

    List<Object> parameters = new ArrayList<Object>();

    for (int i = 0; i < params.length; i++) {
      String paramName = "";
      String paramDefaultValue = "";

      for (Annotation annotation : params[i]) {
        String annotationClass = annotation.annotationType().getName();

        if (annotationClass == "javax.ws.rs.QueryParam") {
          QueryParam param = (QueryParam) annotation;
          paramName = param.value();
        } else if (annotationClass == "javax.ws.rs.DefaultValue") {
          DefaultValue param = (DefaultValue) annotation;
          paramDefaultValue = param.value();
        } else if (annotationClass == "javax.ws.rs.core.Context") {
          if (paramTypes[i] == HttpServletRequest.class) {

            CpfHttpServletRequest cpfRequest = (CpfHttpServletRequest) getRequest();
            for (Map.Entry<String, Object> entry : requestParameters.entrySet()) {
              String key = entry.getKey();

              Object paramValue = entry.getValue();
              String reqValue = null;
              if (paramValue instanceof String[]) {
                String[] lValues = (String[])paramValue;
                if (lValues.length > 0)
                  reqValue = lValues[0];
              } else if (paramValue != null) {
                reqValue = paramValue.toString();
              }

              cpfRequest.setParameter(key, reqValue);
            }

            parameters.add((HttpServletRequest)cpfRequest);
          }
          else if (paramTypes[i] == HttpServletResponse.class) {
            HttpServletResponse response = (HttpServletResponse) getParameterProviders().get("path").getParameter("httpresponse");
            if (response == null) {
              response = getResponse();
            }
            parameters.add(response);
          }
        }
      }

      if (requestParameters.containsKey(paramName)) {
        Object paramValue = requestParameters.get(paramName);
        if (paramTypes[i] == int.class) {
          if (paramValue instanceof String[]) {
            String[] lValues = (String[])paramValue;
            if (lValues.length > 0)
              paramValue = lValues[0];
            else
              paramValue = null;
          }
          int val = Integer.parseInt((String)paramValue);
          parameters.add(val);
        } else if (paramTypes[i] == java.lang.Boolean.class || paramTypes[i] == boolean.class) {

          if (paramValue instanceof String[]) {
            String[] lValues = (String[])paramValue;
            if (lValues.length > 0)
              paramValue = lValues[0];
            else
              paramValue = null;
          }

          boolean val = Boolean.parseBoolean((String)paramValue);
          parameters.add(val);
        } else if (paramTypes[i] == java.util.List.class) {
          List<String> list = new ArrayList<String>();

          String[] splittedValues;
          if (paramValue instanceof String[]) {
            splittedValues = (String[])paramValue;
          } else {
            splittedValues = ((String)paramValue).split(",");
          }


          for (String s : splittedValues) {
            list.add(s);
          }

          parameters.add(list);
        } else if (paramTypes[i] == java.lang.String.class) {
          if (paramValue instanceof String[]) {
            String[] lValues = (String[])paramValue;
            if (lValues.length > 0)
              paramValue = lValues[0];
            else
              paramValue = null;
          }
          parameters.add(paramValue);
        }
        requestParameters.remove(paramName);
      } else {
        if (paramTypes[i] == int.class) {
          int val = Integer.parseInt(paramDefaultValue);
          parameters.add(val);
        } else if (paramTypes[i] == Boolean.class || paramTypes[i] == boolean.class) {
          boolean val = Boolean.parseBoolean(paramDefaultValue);
          parameters.add(val);
        } else if (paramTypes[i] == java.util.List.class) {
          List<String> list = new ArrayList<String>();

          String values = paramDefaultValue;
          String[] splittedValues = values.split(",");

          for (String s : splittedValues) {
            list.add(s);
          }
          parameters.add(list);
        } else if (paramTypes[i] == java.lang.String.class) {
          parameters.add(paramDefaultValue);
        }
      }
    }

    try {
      objectResponse = operation.invoke(o, parameters.toArray());
    } catch (IllegalAccessException ex) {
      logger.error("", ex);
    } catch (IllegalArgumentException ex) {
      logger.error("", ex);
    } catch (InvocationTargetException ex) {
      logger.error("", ex);
    } catch (Exception ex) {
      logger.error("", ex);
    }
  }

  public String call() {
    run();

    CpfHttpServletResponse cpfResponse = (CpfHttpServletResponse) response;
    if (response != null) {
      String content = "";

      try {
        content = cpfResponse.getContentAsString();
      } catch (UnsupportedEncodingException ex) {
        logger.error("Error getting content from CpfHttpServletResponse", ex);
      }
      return content;
    }

    if (objectResponse != null)
      return objectResponse.toString();

    return null;

  }

  public void runInPluginClassLoader() {
    getClassLoaderCaller().runInClassLoader(this);
  }

  public String callInPluginClassLoader() {
    try {
      return getClassLoaderCaller().callInClassLoader(this);
    } catch (Exception e) {
      logger.error(e);
      return null;
    }
  }

  public HttpServletResponse getResponse() {
    if (response == null) {
      logger.debug("No response passed to method " + this.method + ", adding response.");
      response = new CpfHttpServletResponse();
    }

    return response;
  }

  public void setResponse(HttpServletResponse response) {
    this.response = response;
  }

  public void setSession(IPentahoSession session) {
    this.session = session;
  }

  public void setRequestParameters(Map<String, Object> parameters) {
    this.requestParameters = parameters;
  }


  protected IPentahoSession getSession() {
    if (session == null) {
      session = PentahoSessionHolder.getSession();
    }
    return session;
  }

  protected IParameterProvider getRequestParameterProvider() {
    SimpleParameterProvider provider = null;
    if (request != null) {
      provider = new HttpRequestParameterProvider(request);
      provider.setParameters(requestParameters);
    } else {
      provider = new SimpleParameterProvider(requestParameters);
    }
    return provider;
  }

  protected ClassLoaderAwareCaller getClassLoaderCaller() {
    return new ClassLoaderAwareCaller(getPluginManager().getClassLoader(plugin.getTitle()));
  }

  protected IPluginManager getPluginManager() {
    if (pluginManager == null) {
      pluginManager = PentahoSystem.get(IPluginManager.class, getSession());
    }
    return pluginManager;
  }

  protected IParameterProvider getPathParameterProvider() {
    Map<String, Object> pathMap = new HashMap<String, Object>();
    pathMap.put("path", "/" + method);
//    if (response != null) {
    pathMap.put("httpresponse", getResponse());
//    }
    if (getRequest() != null) {
      pathMap.put("httprequest", getRequest());
    }
    IParameterProvider pathParams = new SimpleParameterProvider(pathMap);
    return pathParams;
  }

  protected Map<String, IParameterProvider> getParameterProviders() {
    IParameterProvider requestParams = getRequestParameterProvider();
    IParameterProvider pathParams = getPathParameterProvider();
    Map<String, IParameterProvider> paramProvider = new HashMap<String, IParameterProvider>();
    paramProvider.put(IParameterProvider.SCOPE_REQUEST, requestParams);
    paramProvider.put("path", pathParams);
    return paramProvider;
  }




}