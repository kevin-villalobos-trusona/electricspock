/*
 * Copyright 2016 Herman Cheung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package hkhc.electricspock;

import hkhc.electricspock.internal.ContainedRobolectricTestRunner;
import hkhc.electricspock.internal.ElectricSpockInterceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.robolectric.internal.SdkEnvironment;
import org.spockframework.runtime.Sputnik;
import org.spockframework.runtime.model.SpecInfo;
import spock.lang.Title;

/**
 * Created by herman on 27/12/2016. Test Runner
 */

public class ElectricSputnik extends Runner implements Filterable, Sortable {

  private SdkEnvironment sdkEnvironment;

  /* it is used to setup Robolectric infrastructure, and not used to run actual test cases */
  private ContainedRobolectricTestRunner containedRunner;

  /* the real test runner to run test classes. It is enclosed by ElectricSputnik so that it is
  run within Robolectric interception
   */
  private Runner sputnik;

  static {
    new SecureRandom(); // this starts up the Poller SunPKCS11-Darwin thread early, outside of any Robolectric classloader
  }

  public ElectricSputnik(Class<?> testClass) throws InitializationError {
    containedRunner = new ContainedRobolectricTestRunner();
    sdkEnvironment = containedRunner.getContainedSdkEnvironment();

    // Since we have bootstrappedClass we may properly initialize
    sputnik = createSputnik(testClass);

    registerSpec();

  }

  private Runner createSputnik(Class<?> testClass) {

    Class bootstrappedTestClass = sdkEnvironment.bootstrappedClass(testClass);

    try {

      return (Runner) sdkEnvironment
        .bootstrappedClass(Sputnik.class)
        .getConstructor(Class.class)
        .newInstance(bootstrappedTestClass);

    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  private void registerSpec() {

    Constructor interceptorConstructor = getInterceptorConstructor();

    for (Method method : sputnik.getClass().getDeclaredMethods()) {
      Object spec = getSpec(method);
      if (spec != null) {
        try {
          // ElectricSpockInterceptor self-register on construction, no need to keep a ref here
          interceptorConstructor.newInstance(spec, containedRunner);
        }
        catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }

  private Object getSpec(Method method) {

    if (method.getName().equals("getSpec")) {
      method.setAccessible(true);
      try {
        return method.invoke(sputnik);
      }
      catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return null;
    }
  }

  private Constructor getInterceptorConstructor() {

    try {
      return sdkEnvironment.bootstrappedClass(ElectricSpockInterceptor.class)
        .getConstructor(sdkEnvironment.bootstrappedClass(SpecInfo.class), ContainedRobolectricTestRunner.class);
    }
    catch (NoSuchMethodException e) {
      // it should not happen in production code as the class
      // ElectricSpockInterceptor is known
      throw new RuntimeException(e);
    }

  }

  public Description getDescription() {

    Description originalDesc = sputnik.getDescription();

    Class<?> testClass = originalDesc.getTestClass();
    if (testClass == null) {
      throw new RuntimeException("Unexpected null testClass");
    }

    String title = null;
    Annotation[] annotations = testClass.getAnnotations();
    for (Annotation a : annotations) {
      if (a instanceof Title) {
        title = ((Title) a).value();
        break;
      }
    }

    Description overridedDesc = Description.createSuiteDescription(
      title == null ? testClass.getName() : title
    );
    originalDesc.getChildren().forEach(overridedDesc::addChild);

    return overridedDesc;

  }

  public void run(RunNotifier notifier) {
    sputnik.run(notifier);
  }

  public void filter(Filter filter) throws NoTestsRemainException {
    ((Filterable) sputnik).filter(filter);
  }

  public void sort(Sorter sorter) {
    ((Sortable) sputnik).sort(sorter);
  }

}