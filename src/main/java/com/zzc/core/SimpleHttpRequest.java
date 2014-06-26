package com.zzc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 封装http请求的辅助类,用来完成简单的http异步调用
 * @author zhengzhichao
 *
 */
public class SimpleHttpRequest implements Callable<String>{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final String charset = "UTF-8";
	
	private String url;
	private RequestMethod requestMethod;//post get
	private int timeout;//超时时间
	private Map<String, String> requestParam;//请求参数
	private Map<String,String> requestProperty;//请求属性 比如请求头
	
	/**
	 * 
	 * @param url
	 * @param requestMethod get or post
	 * @param requestParam 请求参数
	 * @param requestProperty 请求附加属性，比如头信息字段
	 * @param timeout 超时时间 0表示不设置
	 */
	public SimpleHttpRequest(String url,RequestMethod requestMethod,Map<String, String> requestParam,Map<String,String> requestProperty,int timeout){
		this.url = url;
		this.requestMethod = requestMethod;
		this.requestParam = requestParam;
		this.requestProperty = requestProperty;
		this.timeout = timeout;
	}
	
	/**
	 * 
	 * @param url
	 * @param requestMethod get or post
	 * @param requestParam 请求参数
	 * @param requestProperty 请求附加属性，比如头信息字段
	 */
	public SimpleHttpRequest(String url,RequestMethod requestMethod,Map<String, String> requestParam,Map<String,String> requestProperty){
		this.url = url;
		this.requestMethod = requestMethod;
		this.requestParam = requestParam;
		this.requestProperty = requestProperty;
	}
	
	/**
	 * 
	 * @param url
	 * @param requestMethod get or post
	 * @param requestParam 请求参数
	 */
	public SimpleHttpRequest(String url,RequestMethod requestMethod,Map<String, String> requestParam){
		this.url = url;
		this.requestMethod = requestMethod;
		this.requestParam = requestParam;
	}
	
	/**
	 * 
	 * @param url
	 * @param requestMethod get or post
	 */
	public SimpleHttpRequest(String url,RequestMethod requestMethod){
		this.url = url;
		this.requestMethod = requestMethod;
	}
	
	@Override
	public String call() throws IOException {
		String retStr = null;
		URL url = null;
		BufferedReader in = null;
		HttpURLConnection httpURLConnection = null;
		try {
			if(this.requestMethod == RequestMethod.GET){//get 请求
				
				//处理请求参数
				if(this.requestParam != null && this.requestParam.size() > 0){
					StringBuilder param = new StringBuilder();
					Iterator<Entry<String, String>> it = this.requestParam.entrySet().iterator();
					while(it.hasNext()){
						Entry<String, String> temp = it.next();
						param.append(temp.getKey()).append("=").append(URLDecoder.decode(temp.getValue(),charset)).append("&");
					}
					String paramStr = param.toString();
					url = new URL(this.url+"?"+paramStr.substring(0,paramStr.length()-1));
				}else{
					url = new URL(this.url);
				}
				
				//打开链接
				httpURLConnection = (HttpURLConnection)url.openConnection();
				httpURLConnection.setRequestMethod("GET");
				if(this.timeout!=0){
					httpURLConnection.setReadTimeout(this.timeout);
				}
				
				//设置请求属性
				if(this.requestProperty != null && this.requestProperty.size() > 0){
					Iterator<Entry<String, String>> it2 = this.requestProperty.entrySet().iterator();
					while(it2.hasNext()){
						Entry<String, String> temp = it2.next();
						httpURLConnection.setRequestProperty(temp.getKey(), temp.getValue());
					}
				}
			}else{//post 请求
				//处理请求参数
				String postData = "";
				if(this.requestParam != null && this.requestParam.size() > 0){
					StringBuilder param = new StringBuilder();
					Iterator<Entry<String, String>> it = this.requestParam.entrySet().iterator();
					while(it.hasNext()){
						Entry<String, String> temp = it.next();
						param.append(temp.getKey()).append("=").append(URLDecoder.decode(temp.getValue(),charset)).append("&");
					}
					String paramStr = param.toString();
					postData = paramStr.substring(0,paramStr.length()-1);
				}
				
				//创建链接
				url = new URL(this.url);
				httpURLConnection = (HttpURLConnection)url.openConnection();
				httpURLConnection.setRequestMethod("POST");
				if(this.timeout!=0){
					httpURLConnection.setReadTimeout(this.timeout);
				}
				
				//设置请求属性
				if(this.requestProperty != null && this.requestProperty.size() > 0){
					Iterator<Entry<String, String>> it2 = this.requestProperty.entrySet().iterator();
					while(it2.hasNext()){
						Entry<String, String> temp = it2.next();
						httpURLConnection.setRequestProperty(temp.getKey(), temp.getValue());
					}
				}
				
				//设置参数并发送请求
				httpURLConnection.setDoOutput(true);
				httpURLConnection.getOutputStream().write(postData.getBytes());
				httpURLConnection.getOutputStream().flush();
				httpURLConnection.getOutputStream().close();
			}
			
			
			//处理请求
			int responseCode = httpURLConnection.getResponseCode();
			logger.debug("----->http response code is "+responseCode);
			if(200 == responseCode){
				in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(),charset));
			    String inline = "";  
			    StringBuilder str = new StringBuilder();
			    while ((inline =in.readLine()) != null){
			    	str.append(inline);
			    }
			    retStr = str.toString();//url返回结果
			    
			    logger.debug("----->response msg is "+retStr);
			}else{
				logger.error("http response code is "+responseCode);
			}
		}catch(IOException e1){
			logger.error(e1.getMessage(),e1);
			throw e1;
		}finally{
			try {
				if(in!=null){
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return retStr;
	}
	
}
