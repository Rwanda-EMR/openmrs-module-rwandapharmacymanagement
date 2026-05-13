package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.openmrs.module.ModuleClassLoader;
import org.openmrs.ui.framework.UiContextRefreshedCallback;
import org.openmrs.ui.framework.fragment.FragmentControllerProvider;
import org.openmrs.ui.framework.fragment.FragmentFactory;
import org.openmrs.ui.framework.fragment.GroovyFragmentViewProvider;
import org.openmrs.ui.framework.page.GroovyPageViewProvider;
import org.openmrs.ui.framework.page.PageControllerProvider;
import org.openmrs.ui.framework.page.PageFactory;
import org.openmrs.ui.framework.resource.ModuleResourceProvider;
import org.openmrs.ui.framework.resource.ResourceFactory;

public class RwandaPharmacyUiConfiguration implements UiContextRefreshedCallback {

    private static final String MODULE_ID = "rwandapharmacymanagement";
    private static final String BASE_PACKAGE = "org.openmrs.module.rwandapharmacymanagement";

    @Override
    public void afterContextRefreshed(PageFactory pageFactory, FragmentFactory fragmentFactory,
                                      ResourceFactory resourceFactory) {
        ClassLoader classLoader = RwandaPharmacyUiConfiguration.class.getClassLoader();

        pageFactory.addControllerProvider(MODULE_ID, new ModuleClasspathPageControllerProvider(classLoader));

        GroovyPageViewProvider pageViewProvider = new GroovyPageViewProvider();
        pageViewProvider.setViewClassLoader(classLoader);
        pageFactory.addViewProvider(MODULE_ID, pageViewProvider);

        fragmentFactory.addControllerProvider(MODULE_ID, new ModuleClasspathFragmentControllerProvider(classLoader));

        GroovyFragmentViewProvider fragmentViewProvider = new GroovyFragmentViewProvider();
        fragmentViewProvider.setViewClassLoader(classLoader);
        fragmentFactory.addViewProvider(MODULE_ID, fragmentViewProvider);

        if (classLoader instanceof ModuleClassLoader) {
            ModuleResourceProvider resourceProvider = new ModuleResourceProvider();
            resourceProvider.setModuleClassLoader((ModuleClassLoader) classLoader);
            resourceFactory.addResourceProvider(MODULE_ID, resourceProvider);
        }
    }

    private abstract static class ModuleClasspathControllerProvider {

        private final ClassLoader classLoader;
        private final String basePackage;
        private final String suffix;

        private ModuleClasspathControllerProvider(ClassLoader classLoader, String basePackage, String suffix) {
            this.classLoader = classLoader;
            this.basePackage = basePackage;
            this.suffix = suffix;
        }

        protected Object getControllerForName(String name) {
            String className = basePackage + "." + name.replaceAll("/", ".") + suffix;
            try {
                return classLoader.loadClass(capitalizeLastSegment(className)).newInstance();
            }
            catch (ClassNotFoundException ex) {
                return null;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private String capitalizeLastSegment(String className) {
            int lastDot = className.lastIndexOf(".");
            if (lastDot < 0 || lastDot + 1 >= className.length()) {
                return className;
            }
            return className.substring(0, lastDot + 1)
                    + Character.toUpperCase(className.charAt(lastDot + 1))
                    + className.substring(lastDot + 2);
        }
    }

    private static class ModuleClasspathPageControllerProvider extends ModuleClasspathControllerProvider
            implements PageControllerProvider {

        private ModuleClasspathPageControllerProvider(ClassLoader classLoader) {
            super(classLoader, BASE_PACKAGE + ".page.controller", "PageController");
        }

        @Override
        public Object getController(String pageName) {
            return getControllerForName(pageName);
        }
    }

    private static class ModuleClasspathFragmentControllerProvider extends ModuleClasspathControllerProvider
            implements FragmentControllerProvider {

        private ModuleClasspathFragmentControllerProvider(ClassLoader classLoader) {
            super(classLoader, BASE_PACKAGE + ".fragment.controller", "FragmentController");
        }

        @Override
        public Object getController(String fragmentName) {
            return getControllerForName(fragmentName);
        }
    }
}
