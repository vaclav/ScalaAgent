//  ScalaAgent
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.scalaagent

import actors.Actor
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch

/**
 * The Agent class was strongly inspired by the agent principle in Clojure. Essentially, an agent wraps a shared mutable state
 * and hides it behind a message-passing interface. Agents accept messages and process them on behalf of the wrapped state.
 * Typically agents accept functions / commands as messages and ensure the submitted commands are executed against the internal
 * agent's state in a thread-safe manner (sequentially).
 * The submitted functions / commands take the internal state as a parameter and their output becomes the new internal state value.
 * The code that is submitted to an agent doesn't need to pay attention to threading or synchronization, the agent will
 * provide such guarantees by itself.
 * See the examples of use for more details.
 *
 * @author Vaclav Pech
 * Date: Oct 18, 2009
 */
class Agent[T](var data: T, val copyStrategy: CopyStrategy[T], val errorHandler: (Throwable => Boolean)) extends Actor {
    def this(data: T) = {this (data, new IdentityCopyStrategy[T], defaultErrorHandler)}

    def this(data: T, copyStrategy: CopyStrategy[T]) = {
        this (data, copyStrategy, defaultErrorHandler)
    }

    /**
     * Periodically handles incoming messages
     */
    final def act() {
        react {
            case FunctionHolder(fun: (T => T)) => {
                updateData(fun(data))
            }
            case ValueHolder(x: T) => {
                updateData(x)
            }
            case ProcedureHolder(fun: (T => Unit)) => {
                try {
                    fun(copyStrategy(data))
                    act()
                } catch {
                    case e: RuntimeException => {
                        if (errorHandler(e)) {
                            act()
                        }
                        else this.exit()
                    }
                }
            }
            case x: Any => {
                if (errorHandler(new RuntimeException("Unknown message type received: " + x))) act()
                else this.exit()
            }
        }
    }


    /**
     * Updates the internal state with the value provided as a by-name parameter
     */
    private final def updateData(newData: =>T) {
        try {
            data = newData
            act()
        } catch {
            case e: RuntimeException => {
                if (errorHandler(e)) {
                    act()
                }
                else this.exit()
            }
        }
    }

    /**
     * Submits a request to read the internal state.
     * A copy of the internal state will be returned, depending on the underlying effective copyStrategy.
     * Internally leverages the asynchronous getValue() method and then waits for its result on a CountDownLatch.
     */
    final def get : T = {
        val ref: AtomicReference[T] = new AtomicReference[T]()
        val latch: CountDownLatch = new CountDownLatch(1)
        get((x: T) => {ref.set(x); latch.countDown})
        latch.await
        return ref.get
    }

    /**
     * Asynchronously submits a request to read the internal state. The supplied function will be executed on the returned internal state value.
     * A copy of the internal state will be used, depending on the underlying effective copyStrategy.
     */
    final def get(message: (T => Unit)) {
        this ! ProcedureHolder(message)
    }

    /**
     * Submits the provided function for execution against the internal agent's state
     */
    final def apply(message: (T => T)) {
        this ! FunctionHolder(message)
    }

    /**
     * Submits a new value to be set as the new agent's internal state
     */
    final def apply(message: T) {
        this ! ValueHolder(message)
    }

    /**
     * The internal messages for passing around requests
     */
    private case class FunctionHolder[T](val fun: ((T) => T))
    private case class ProcedureHolder[T](val fun: ((T) => Unit))
    private case class ValueHolder[T](val value: T)
}

/**
 * Provides factory methods to create Agents.
 */
object Agent {
    def apply[T](data:T) = {
        new Agent(data)
    }
    def apply[T](data:T, copyStrategy:CopyStrategy[T]) = {
        new Agent(data, copyStrategy)
    }
    def apply[T](data:T, copyStrategy:CopyStrategy[T], errorHandler: (Throwable => Boolean)) = {
        new Agent(data, copyStrategy, errorHandler)
    }
}

/**
 * A default error handler, which only prints out the error stacktrace and stops the agent.
 */
private object defaultErrorHandler extends Function[Throwable, Boolean] {
    /**
     * Calls when an exception occurs in the agent's body.
     * @return True, if the agent should continue processing messages, false if the agent should stop immediately.
     */
    def apply(x: Throwable): Boolean = {
        x.printStackTrace(System.err)
        return false
    }
}

/**
 * A general strategy to create copies of internal data to send in reply to read requests.
 */
abstract class CopyStrategy[T] extends Function[T, T]

/**
 * Returns the original piece of data
 */
class IdentityCopyStrategy[T] extends CopyStrategy[T] {
    def apply(x: T): T = {x}
}
