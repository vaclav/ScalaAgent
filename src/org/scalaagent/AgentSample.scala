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

import scala.collection.mutable.HashMap


/**
 *
 * @author Vaclav Pech
 * Date: Oct 18, 2009
 */

/**
 * Stores a list of names and allows concurrent threads to manipulate the list.
 */
object AgentSample {
    def main(args: Array[String]) {
        val agent: Agent[List[String]] = new Agent[List[String]](List("Joe"))
        agent.start()
        agent(List("Dave")) //Set the state to a new value
        agent((x: List[String]) => "Alice" :: x) //Pre-pend a value to the internal list
        agent.getValue((x: List[String]) => x.foreach(println _)) //Print the content asynchronously
    }
}

/**
 * Wraps a counter allowing other threads to safely update the counter concurrently
 */
object CounterSample {
    def increment(x: Long) = x + 1

    def decrement(x: Long) = x - 1

    def main(args: Array[String]) {
        val agent: Agent[Long] = new Agent[Long](0L)
        agent.start()
        agent((x: Long) => x + 100) //Increment the value by 100
        agent((x: Long) => increment(x)) //Increment the value by 1 calling the increment function
        //        agent(decrement)  //todo enable
        println(agent.getValue()) //Print the content

    }
}

/**
 * A thread-safe shopping cart wrapping the internal state represented as a HashMap inside an Agent.
 * All public method calls on the ShoppingCart are transformed into a command execution on the internal agent.
 */
class ShoppingCart {
    val content: Agent[HashMap[String, Int]] = new Agent[HashMap[String, Int]](new HashMap())

    def start() { content.start()}

    def getContent() : HashMap[String, Int] = { content.getValue }

    def addItem(product: String) {
        content((x: HashMap[String, Int]) => {
            x += product -> 1
            x
        })
    }

    def removeItem(product: String) {
        content((x: HashMap[String, Int]) => {
            x -= product
            x
        })
    }

    def clear() {
        content((x: HashMap[String, Int]) => {
            x.clear()
            x
        })
    }
}

object ShoppingCartExample {
    def main(args: Array[String]) {
        val cart: ShoppingCart = new ShoppingCart
        cart.start()

        cart.addItem("Budweiser")
        cart.addItem("Pilsner")
        cart.addItem("Staropramen")
        cart.removeItem("Budweiser")

        println(cart.getContent())
        cart.clear()
        println(cart.getContent())
    }
}