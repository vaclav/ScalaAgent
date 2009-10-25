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
        agent start()
        agent(List("Dave"))                      //Set the state to a new value
        agent((x: List[String]) => "Alice" :: x) //Pre-pend a value to the internal list
        agent("Susan" :: _)                      //Pre-pend a value to the internal list
        agent getValue(_.foreach(println _))     //Print the content asynchronously
    }
}

/**
 * Wraps a counter allowing other threads to safely update the counter concurrently
 */
object CounterSample {

    def increment(x: Long) = x + 1
    def decrement(amount : Long)(x: Long) = x - amount

    def main(args: Array[String]) {
        val agent: Agent[Long] = new Agent[Long](0L)    //Wrap a counter by an Agent
        agent start

        agent((x: Long) => x + 100) //Increment the value by 100
        agent(_ + 100)              //Increment the value by 100

        new Thread(new Runnable() { //Start a new thread to manipulate the counter in parallel
            def run = {
                    agent(increment(_)) //Increment the value by 1 calling the increment function
                }
        }).start()

        agent(increment _)          //Send a method that will increment the counter by 1
        agent(decrement(3)_)        //Send a method that will decrement the counter by 3

        println(agent getValue) //Print the content
    }
}

/**
 * A thread-safe shopping cart wrapping the internal state represented as a HashMap inside an Agent.
 * All public method calls on the ShoppingCart are transformed into a command execution on the internal agent.
 */
class ShoppingCart {
    val content: Agent[HashMap[String, Int]] = new Agent[HashMap[String, Int]](new HashMap())

    def start() { content start}

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
            x.clear
            x
        })
    }

    /**
     * Curry the changeQuantity() private method and send it off to the Agent for processing
     */
    def increaseQuantity(product:String, quantity:Int) {
        content(changeQuantity(product, quantity, _:HashMap[String, Int]))
    }

    /**
     * Manipulates the cart's map directly, since it is never called directly by clients, but always indirectly
     * inside the Agent's thread.
     */
    private def changeQuantity(product:String, quantity:Int, items:HashMap[String, Int]) : HashMap[String, Int] = {
        items(product) = (if (items.contains(product)) items(product) else 0) + quantity
        items
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

object CustomCopyStrategyExample {

    object MyIntCopyStrategy extends CopyStrategy[Int] {
        def apply(x:Int) : Int = {
            def a = x
            return a
        }
    }

    def main(args: Array[String]) {
        val agent: Agent[Int] = new Agent[Int](0, MyIntCopyStrategy)
        agent start

        agent(_ + 10)
        agent(_ + 20)

        println(agent getValue)
    }
}

object NumberExample {
    def main(args: Array[String]) {
        val agent: Agent[List[Int]] = new Agent[List[Int]](List(10))
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
//        agent.getValue((x: List[Int]) => {throw new RuntimeException("read test")})
        agent.getValue((x: List[Int]) => {println("Value: " + x)})
        println("Result: " + agent.getValue())
    }
}
