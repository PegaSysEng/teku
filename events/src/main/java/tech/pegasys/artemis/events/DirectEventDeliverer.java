/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.artemis.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class DirectEventDeliverer<T> extends EventDeliverer<T> {
  private static final Logger LOG = LogManager.getLogger();

  @Override
  protected void deliverTo(final T subscriber, final Method method, final Object[] args) {
    try {
      method.invoke(subscriber, args);
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOG.error("Failed to deliver " + method.getName() + " event to " + subscriber.getClass(), e);
    }
  }
}
