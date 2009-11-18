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

import scala.actors.Actor._


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
        val agent = Agent(List("Joe"))
        agent start()
        agent(List("Dave"))                      //Set the state to a new value
        agent((x: List[String]) => "Alice" :: x) //Pre-pend a value to the internal list
        agent("Susan" :: _)                      //Pre-pend a value to the internal list
        agent get(_.foreach(println _))     //Print the content asynchronously
    }
}

/**
 * Wraps a counter allowing other threads to safely update the counter concurrently
 */
object CounterSample {

    def increment(x: Long) = x + 1
    def decrement(amount : Long)(x: Long) = x - amount

    def main(args: Array[String]) {
        val agent = Agent(0L)    //Wrap a counter by an Agent
        agent start

        agent((x: Long) => x + 100) //Increment the value by 100
        agent{_ + 100}              //Increment the value by 100
        def a = actor {
            agent(_ + 100)              //Increment the value by 100 from within an actor
        }
        a.start

        new Thread(new Runnable() { //Start a new thread to manipulate the counter in parallel
            def run = {
                    agent(increment(_)) //Increment the value by 1 calling the increment function
                }
        }).start

        agent(increment _)           //Send a method that will increment the counter by 1
        agent(decrement(3) _)        //Send a method that will decrement the counter by 3

        println(agent get)               //Print the content
    }
}

/**
 * A thread-safe shopping cart wrapping the internal state represented as a HashMap inside an Agent.
 * All public method calls on the ShoppingCart are transformed into a command execution on the internal agent.
 */
class ShoppingCart {
    val content = Agent(Map[String, Int]())

    def start() { content start}

    def getContent() : Map[String, Int] = { content.get }

    def addItem(product: String) {
        content((x: Map[String, Int]) => {
            x + product -> 1
        })
    }

    def removeItem(product: String) {
        content((x: Map[String, Int]) => {
            x - product
        })
    }

    def clear() {
        content((x: Map[String, Int]) => {
            Map[String, Int]()
        })
    }

    /**
     * Curry the changeQuantity() private method and send it off to the Agent for processing
     */
    def increaseQuantity(product:String, quantity:Int) {
        content(changeQuantity(product, quantity, _:Map[String, Int]))
    }

    /**
     * Manipulates the cart's map directly, since it is never called directly by clients, but always indirectly
     * inside the Agent's thread.
     */
    private def changeQuantity(product:String, quantity:Int, items:Map[String, Int]) = {
        if (items.contains(product)) {
            def currentQuantity = items(product)
            items + product -> (currentQuantity + quantity)
        } else items
    }
}

object ShoppingCartExample {
    def main(args: Array[String]) {
        val cart: ShoppingCart = new ShoppingCart
        cart start

        cart addItem "Budweiser" 
        cart addItem "Pilsner"
        cart addItem "Staropramen"
        cart removeItem "Budweiser"

        println(cart getContent)
        cart increaseQuantity("Staropramen", 5)
        println(cart getContent)
        cart clear()
        println(cart getContent)

    }
}

/**
 * Uses a custom copy strategy to avoid returning the original state instance.
 */
object CustomCopyStrategyExample {

    object MyIntCopyStrategy extends CopyStrategy[Int] {
        def apply(x:Int) : Int = {
            def a = x
            return a
        }
    }

    def main(args: Array[String]) {
        val agent = Agent(0, MyIntCopyStrategy)
        agent start

        agent(_ + 10)
        agent(_ + 20)

        println(agent get)
    }
}

/**
 * Shows (after un-commenting) the default error handling mechanics.
 */
object NumberExample {
    def main(args: Array[String]) {
        val agent = Agent(List(10))
        agent.start()
        agent(List[Int](1))
        agent((x: List[Int]) => 2 :: x)
        agent((x: List[Int]) => 3 :: x)
        agent((x: List[Int]) => x.reverse)
//        agent((x: List[Int]) => {
//            if (true) throw new RuntimeException("test")
//            return List()
//        })
        agent((x: List[Int]) => {
            x.map(_ * 2)
        })
//        agent.get((x: List[Int]) => {throw new RuntimeException("read test")})
        agent.get((x: List[Int]) => {println("Value: " + x)})
        println("Result: " + agent.get)
    }
}
