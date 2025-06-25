package com.sap.cap.ai2code.service.bot;

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.service.bot.BotExecution;

/**
 * BotExecution 工厂服务
 * 负责创建 BotExecution 实例并处理依赖注入
 */
@Service
public class BotExecutionFactoryService {
    
    private final ApplicationContext applicationContext;
    
    // 预定义的包路径，用于搜索类
    private static final String[] SEARCH_PACKAGES = {
        "com.sap.cap.ai2code.service.bot",
        // 可以根据需要添加更多包路径
    };

    public BotExecutionFactoryService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 创建 BotExecution 实例 - 支持构造函数注入
     */
    public BotExecution createBotExecutionInstance(String className) {
        try {
            Class<?> clazz = resolveClass(className);

            // 检查是否实现了 BotExecution 接口
            if (!BotExecution.class.isAssignableFrom(clazz)) {
                throw new BusinessException("Class " + className + " does not implement BotExecution interface");
            }

            // 方案1：优先尝试从 Spring 容器中获取已配置的 Bean
            try {
                if (applicationContext.getBeanNamesForType(clazz).length > 0) {
                    BotExecution instance = (BotExecution) applicationContext.getBean(clazz);
                    System.out.println("- Retrieved BotExecution instance from Spring container: " + clazz.getName());
                    return instance;
                }
            } catch (Exception e) {
                System.out.println("- Failed to get instance from Spring container, trying manual creation: " + e.getMessage());
            }

            // 方案2：手动创建实例，支持构造函数依赖注入
            return createInstanceWithDependencyInjection(clazz);

        } catch (ClassNotFoundException e) {
            throw new BusinessException("Implementation class not found: " + className + ". Searched in packages: " + String.join(", ", SEARCH_PACKAGES), e);
        } catch (Exception e) {
            throw new BusinessException("Failed to create bot execution instance: " + className, e);
        }
    }

    /**
     * 解析类名 - 支持简单类名和完整类名
     */
    private Class<?> resolveClass(String className) throws ClassNotFoundException {
        // 1. 如果已经是完整类名，直接尝试加载
        if (className.contains(".")) {
            return Class.forName(className);
        }

        // 2. 如果是简单类名，在预定义的包中搜索
        for (String packageName : SEARCH_PACKAGES) {
            try {
                String fullClassName = packageName + "." + className;
                Class<?> clazz = Class.forName(fullClassName);
                System.out.println("- Found class: " + fullClassName);
                return clazz;
            } catch (ClassNotFoundException e) {
                // 继续搜索下一个包
            }
        }

        // 3. 尝试扫描 Spring 容器中的所有 BotExecution 实现类
        return findClassInSpringContext(className);
    }

    /**
     * 在 Spring 容器中查找 BotExecution 实现类
     */
    private Class<?> findClassInSpringContext(String className) throws ClassNotFoundException {
        try {
            // 获取所有 BotExecution 类型的 Bean
            String[] beanNames = applicationContext.getBeanNamesForType(BotExecution.class);
            
            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = bean.getClass();
                
                // 检查简单类名是否匹配
                if (beanClass.getSimpleName().equals(className)) {
                    System.out.println("- Found class in Spring context: " + beanClass.getName());
                    return beanClass;
                }
            }
            
            throw new ClassNotFoundException("Class not found in Spring context: " + className);
            
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to search in Spring context for: " + className, e);
        }
    }

    /**
     * 手动创建实例并注入依赖
     */
    @SuppressWarnings("unchecked")
    private BotExecution createInstanceWithDependencyInjection(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getConstructors();

        // 1. 优先尝试带参数的构造函数
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();

            if (paramTypes.length > 0) {
                Object[] args = resolveConstructorArgs(paramTypes);
                if (args != null) {
                    System.out.println("- Creating BotExecution instance with " + paramTypes.length + " constructor arguments: " + clazz.getSimpleName());
                    return (BotExecution) constructor.newInstance(args);
                }
            }
        }

        // 2. 如果没有合适的带参构造函数，尝试无参构造函数
        try {
            Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
            System.out.println("- Creating BotExecution instance with default constructor: " + clazz.getSimpleName());
            return (BotExecution) defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new BusinessException("No suitable constructor found for class: " + clazz.getName());
        }
    }

    /**
     * 解析构造函数参数
     */
    private Object[] resolveConstructorArgs(Class<?>[] paramTypes) {
        Object[] args = new Object[paramTypes.length];

        try {
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];

                // 尝试从 Spring 容器中获取对应类型的 Bean
                Object bean = resolveBeanFromContext(paramType);
                if (bean != null) {
                    args[i] = bean;
                    System.out.println("  - Resolved constructor parameter " + (i + 1) + ": " + paramType.getSimpleName());
                } else {
                    // 如果无法解析某个参数，返回 null（表示这个构造函数不可用）
                    System.out.println("  - Failed to resolve constructor parameter " + (i + 1) + ": " + paramType.getSimpleName());
                    return null;
                }
            }

            return args;

        } catch (Exception e) {
            System.err.println("Error resolving constructor arguments: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从 Spring 容器中解析指定类型的 Bean
     */
    private Object resolveBeanFromContext(Class<?> type) {
        try {
            // 尝试按类型获取 Bean
            String[] beanNames = applicationContext.getBeanNamesForType(type);

            if (beanNames.length == 1) {
                Object bean = applicationContext.getBean(beanNames[0]);
                System.out.println("    - Found single bean for type " + type.getSimpleName() + ": " + beanNames[0]);
                return bean;
            } else if (beanNames.length > 1) {
                // 如果有多个同类型的 Bean，尝试按名称获取
                String preferredBeanName = getBeanNameByType(type);
                if (preferredBeanName != null && applicationContext.containsBean(preferredBeanName)) {
                    Object bean = applicationContext.getBean(preferredBeanName);
                    System.out.println("    - Found preferred bean for type " + type.getSimpleName() + ": " + preferredBeanName);
                    return bean;
                }
                // 返回第一个找到的 Bean
                Object bean = applicationContext.getBean(beanNames[0]);
                System.out.println("    - Found multiple beans for type " + type.getSimpleName() + ", using first: " + beanNames[0]);
                return bean;
            }

            // 如果找不到，尝试按接口类型查找
            return tryResolveByInterface(type);

        } catch (Exception e) {
            System.err.println("Failed to resolve bean for type: " + type.getName() + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据类型推断 Bean 名称
     */
    private String getBeanNameByType(Class<?> type) {
        String className = type.getSimpleName();

        // 常见的命名约定
        if (className.endsWith("Service")) {
            return className.substring(0, 1).toLowerCase() + className.substring(1);
        } else if (className.endsWith("Repository")) {
            return className.substring(0, 1).toLowerCase() + className.substring(1);
        } else if (className.endsWith("Dao")) {
            return className.substring(0, 1).toLowerCase() + className.substring(1);
        } else if (className.endsWith("Manager")) {
            return className.substring(0, 1).toLowerCase() + className.substring(1);
        } else if (className.endsWith("Handler")) {
            return className.substring(0, 1).toLowerCase() + className.substring(1);
        }

        return null;
    }

    /**
     * 尝试通过接口类型解析 Bean
     */
    private Object tryResolveByInterface(Class<?> type) {
        try {
            // 检查是否是接口
            if (type.isInterface()) {
                String[] beanNames = applicationContext.getBeanNamesForType(type);
                if (beanNames.length > 0) {
                    Object bean = applicationContext.getBean(beanNames[0]);
                    System.out.println("    - Resolved interface " + type.getSimpleName() + " with implementation: " + beanNames[0]);
                    return bean;
                }
            }

            // 检查父类和接口
            Class<?>[] interfaces = type.getInterfaces();
            for (Class<?> interfaceType : interfaces) {
                String[] beanNames = applicationContext.getBeanNamesForType(interfaceType);
                if (beanNames.length > 0) {
                    Object bean = applicationContext.getBean(beanNames[0]);
                    System.out.println("    - Resolved via parent interface " + interfaceType.getSimpleName() + ": " + beanNames[0]);
                    return bean;
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error resolving by interface for type: " + type.getName());
            return null;
        }
    }



    /**
     * 获取 BotExecution 实例的详细信息
     */
    public String getBotExecutionInstanceInfo(BotExecution instance) {
        if (instance == null) {
            return "BotExecution instance is null";
        }

        StringBuilder info = new StringBuilder();
        info.append("BotExecution Instance Info:\n");
        info.append("- Class: ").append(instance.getClass().getName()).append("\n");
        info.append("- Simple Name: ").append(instance.getClass().getSimpleName()).append("\n");
        info.append("- Package: ").append(instance.getClass().getPackage().getName()).append("\n");
        
        // 获取实现的接口
        Class<?>[] interfaces = instance.getClass().getInterfaces();
        if (interfaces.length > 0) {
            info.append("- Interfaces: ");
            for (int i = 0; i < interfaces.length; i++) {
                info.append(interfaces[i].getSimpleName());
                if (i < interfaces.length - 1) {
                    info.append(", ");
                }
            }
            info.append("\n");
        }

        return info.toString();
    }
}