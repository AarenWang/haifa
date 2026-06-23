package org.wrj.allspring.version4.rpc;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnableMyRpcClients} 对应的 Registrar。
 * <p>
 * 负责在 Spring 启动早期扫描指定包下带 {@link MyRpcClient} 的接口，并将其包装为
 * {@link MyRpcClientFactoryBean} 的 BeanDefinition 注册到容器中，使业务代码可以通过
 * {@code @Autowired} 直接注入 RPC 客户端代理。
 */
public class MyRpcClientRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> basePackages = resolveBasePackages(importingClassMetadata);
        if (basePackages.isEmpty()) {
            return;
        }

        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MyRpcClient.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                if (!(candidate instanceof AnnotatedBeanDefinition)) {
                    continue;
                }
                AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidate;
                registerRpcClient(beanDefinition, registry);
            }
        }
    }

    /**
     * 解析需要扫描的基础包路径。若用户未配置，则默认使用被 {@link EnableMyRpcClients} 修饰的配置类所在包。
     */
    private Set<String> resolveBasePackages(AnnotationMetadata importingClassMetadata) {
        Set<String> basePackages = new HashSet<>();
        Map<String, Object> attributesMap = importingClassMetadata
                .getAnnotationAttributes(EnableMyRpcClients.class.getName());
        if (attributesMap == null) {
            basePackages.add(getDefaultPackage(importingClassMetadata));
            return basePackages;
        }

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributesMap);
        if (attributes == null) {
            basePackages.add(getDefaultPackage(importingClassMetadata));
            return basePackages;
        }

        for (String basePackage : attributes.getStringArray("basePackages")) {
            if (StringUtils.hasText(basePackage)) {
                basePackages.add(basePackage);
            }
        }

        if (basePackages.isEmpty()) {
            basePackages.add(getDefaultPackage(importingClassMetadata));
        }
        return basePackages;
    }

    private String getDefaultPackage(AnnotationMetadata importingClassMetadata) {
        String className = importingClassMetadata.getClassName();
        int lastDotIndex = className.lastIndexOf('.');
        return lastDotIndex < 0 ? "" : className.substring(0, lastDotIndex);
    }

    /**
     * 注册单个 RPC 客户端：使用接口的 Class 构造 {@link MyRpcClientFactoryBean} 的 BeanDefinition。
     */
    private void registerRpcClient(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
        String interfaceClassName = beanDefinition.getMetadata().getClassName();
        Class<?> interfaceClass;
        try {
            interfaceClass = ClassUtils.forName(interfaceClassName, resourceLoader.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException("无法加载 RPC 客户端接口: " + interfaceClassName, e);
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(MyRpcClientFactoryBean.class);
        builder.addPropertyValue("interfaceClass", interfaceClass);

        String beanName = generateBeanName(interfaceClassName);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private String generateBeanName(String interfaceClassName) {
        String shortName = interfaceClassName.substring(interfaceClassName.lastIndexOf('.') + 1);
        return Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
    }

    /**
     * 重写扫描器校验逻辑：Spring 默认只把具体类当作候选组件，而 RPC 客户端通常是接口，
     * 因此这里允许顶层接口通过校验。
     */
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
            }
        };
    }
}
