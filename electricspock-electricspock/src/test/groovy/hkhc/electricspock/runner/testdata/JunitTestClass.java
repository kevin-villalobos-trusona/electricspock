package hkhc.electricspock.runner.testdata;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by hermanc on 6/4/2017. As test data of hkhc.electricspock.runner.DisplayableSpec
 */

public class JunitTestClass {

  @Test
  public void testOne() {

  }

  @Test
  public void testTwo() {

  }

  @Test
  @Ignore
  public void testIgnored() {

  }

  @Test(expected = AssertionError.class)
  public void testFail() {
    assertEquals(1, 2);
  }

}
