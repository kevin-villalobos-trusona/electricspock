package hkhc.electricspock.internal;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.AndroidSandbox;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

import java.lang.reflect.Method;

/**
 * Created by hermanc on 31/3/2017.
 */

public class ContainedRobolectricTestRunner extends RobolectricTestRunner {

  private FrameworkMethod placeholderMethod = null;
  private AndroidSandbox androidSandbox = null;

  /*
  A place holder test class to obtain a proper FrameworkMethod (which is actually a
  RoboFrameworkTestMethod) by reusing existing code in RobolectricTestRunner
   */
  public static class PlaceholderTest {

    /* Just a placeholder, the actual content of the test method is not important */
    @Test
    @Config(manifest = Config.NONE)
    public void testPlaceholder() {
    }
  }

  /*
  Pretend to be a test runner for the placeholder test class. We don't actually run that test
  method. Just use it to trigger all initialization of Robolectric infrastructure, and use it
  for running Spock specification.
   */
  public ContainedRobolectricTestRunner() throws InitializationError {
    super(PlaceholderTest.class);
  }

  private FrameworkMethod getPlaceHolderMethod() {
    if (placeholderMethod == null) {
      placeholderMethod = getChildren().get(0);
    }

    return placeholderMethod;

  }

  private Method getBootstrappedMethod() {
    FrameworkMethod placeholderMethod = getPlaceHolderMethod();
    AndroidSandbox androidSandbox = getContainedSdkEnvironment();

    // getTestClass().getJavaClass() should always be PlaceholderTest.class,
    // load under Robolectric's class loader
    Class<?> bootstrappedTestClass = androidSandbox.bootstrappedClass(getTestClass().getJavaClass());

    final Method bootstrappedMethod;
    try {
      // getMethod should always be the "testPlaceholder" method.
      bootstrappedMethod = bootstrappedTestClass.getMethod(placeholderMethod.getMethod().getName());
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    return bootstrappedMethod;
  }

  /*
  Override to add itself to doNotAcquireClass, so as to avoid classloader conflict
   */
  @NotNull
  @Override
  protected InstrumentationConfiguration createClassLoaderConfig(final FrameworkMethod method) {
    return new InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
      .doNotAcquireClass(getClass())
      .build();
  }

  public AndroidSandbox getContainedSdkEnvironment() {
    if (androidSandbox == null) {
      FrameworkMethod frameworkMethod = getPlaceHolderMethod();
      androidSandbox = getSandbox(frameworkMethod);
      configureSandbox(androidSandbox, frameworkMethod);
    }

    return androidSandbox;
  }

  public void containedBeforeTest() throws Throwable {
    super.beforeTest(getContainedSdkEnvironment(), getPlaceHolderMethod(), getBootstrappedMethod());
  }

  public void containedAfterTest() {
    super.afterTest(getPlaceHolderMethod(), getBootstrappedMethod());
  }
}