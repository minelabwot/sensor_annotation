package com.yyn.domain;

import org.springframework.aop.BeforeAdvice;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.yyn.model.Test;

public class test1 {
	public static void main(String[] args) {
		FileSystemXmlApplicationContext factory = new FileSystemXmlApplicationContext("file/beans.xml");
		
//		FileSystemResource res = new FileSystemResource("file/beans.xml");
//		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
//		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
//		reader.loadBeanDefinitions(res);
		Test test = (Test)factory.getBean("yyn_Test");
		System.out.println(test.getA()+test.getB());
//		SimpleInstantiationStrategy
//		Thread
//		Beanw
//		ContextLoaderListener
//		XmlWebApplicationContext
//		DispatcherServlet
//		BeanNameUrlHandlerMapping
//		HandlerExecutionChain
		//AbstractHandlerMethodMapping<T>
//		SimpleControllerHandlerAdapter
//		Controller
		//InternalResourceViewResolver
		//MethodBeforeAdvice
	}
		
}

class MyFactoryBean<T> implements FactoryBean<T> {
	@Override
	public T getObject() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Class<?> getObjectType() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}
}
