package bichel.yauhen.hotel.api.di;

import bichel.yauhen.hotel.api.di.annotation.Autowired;
import bichel.yauhen.hotel.api.di.annotation.Component;
import bichel.yauhen.hotel.api.di.annotation.Controller;
import bichel.yauhen.hotel.api.di.annotation.Repository;
import bichel.yauhen.hotel.api.di.annotation.Service;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The injector is called only once at startup to inject dependencies into the class.
 */
public class DependencyInjector {
    private Map<Class<?>, Class<?>> interfaceClassMap;
    private Map<Class<?>, Object> appScope;

    private static DependencyInjector injector;

    private DependencyInjector() {
        interfaceClassMap = new HashMap<>();
        appScope = new HashMap<>();
    }
    public static void startApplication(Class<?> mainClass) {
        try {
            synchronized (DependencyInjector.class) {
                if (injector == null) {
                    injector = new DependencyInjector();
                    injector.initDI(mainClass);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static <T> T getService(Class<T> bean) {
        try {
            return injector.getBeanInstance(bean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public <T> T getBeanInstance(Class<T> interfaceClass) {
        Class<?> implementationClass = getImplementationClass(interfaceClass);

        if (appScope.containsKey(implementationClass)) {
            return (T) appScope.get(implementationClass);
        }

        final String errorMessage = "There is no " + implementationClass.getName();
        throw new RuntimeErrorException(new Error(errorMessage));
    }

    private Class<?> getImplementationClass(Class<?> interfaceClass) {
        Set<Map.Entry<Class<?>, Class<?>>> implementationClasses = interfaceClassMap.entrySet().stream()
                .filter(entry -> entry.getValue() == interfaceClass).collect(Collectors.toSet());
        String errorMessage = "";
        if (implementationClasses == null || implementationClasses.size() == 0) {
            errorMessage = "no implementation found for interface " + interfaceClass.getName();
        } else if (implementationClasses.size() == 1) {
            Optional<Map.Entry<Class<?>, Class<?>>> optional = implementationClasses.stream().findFirst();
            if (optional.isPresent()) {
                return optional.get().getKey();
            }
        } else if (implementationClasses.size() > 1) {
            errorMessage = "There are " + implementationClasses.size() + " of interface " + interfaceClass.getName();
        }

        throw new RuntimeErrorException(new Error(errorMessage));
    }

    public Set<Class> getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassesContainer classesContainer = new ClassesContainer();
        return classesContainer.getClasses(packageName);
    }

    private void initDI(Class<?> mainClass) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
        Set<Class> classes = getClasses(mainClass.getPackage().getName());

        for (Class<?> implementationClass : classes) {
            Class<?>[] interfaces = implementationClass.getInterfaces();
            if (interfaces.length == 0) {
                interfaceClassMap.put(implementationClass, implementationClass);
            } else {
                for (Class<?> interfaze : interfaces) {
                    interfaceClassMap.put(implementationClass, interfaze);
                }
            }
        }

        Queue<Class> queue = new LinkedList<>();
        do {
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(Component.class) ||
                        clazz.isAnnotationPresent(Service.class) ||
                        clazz.isAnnotationPresent(Repository.class) ||
                        clazz.isAnnotationPresent(Controller.class)) {

                    Constructor[] constructors = clazz.getConstructors();
                    for (Constructor constructor : constructors) {
                        if (constructor.isAnnotationPresent(Autowired.class)) {
                            Class[] parameters = constructor.getParameterTypes();
                            for(Class parameter: parameters) {

                            }
                        }
                    }

                    Object classInstance = clazz.newInstance();
                    appScope.put(clazz, classInstance);
                    //autowire(clazz, classInstance);
                    //Object fieldInstance = injector.getBeanInstance(field.getType(), field.getName(), qualifier);
                }
            }
        } while (!queue.isEmpty());
    }
}
