/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

package au.csiro.pathling.encoding;

import java.io.Serializable;

/**
 * Used for representing results of functions that return boolean values.
 *
 * @author John Grimes
 */
public class IdAndBoolean implements Serializable {

  private String id;
  private boolean value;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isValue() {
    return value;
  }

  public void setValue(boolean value) {
    this.value = value;
  }

}