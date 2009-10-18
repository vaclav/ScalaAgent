package org.scalaagent

import actors.Actor
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch

case class FunctionHolder[T](val fun: ((T) => T))
case class ProcedureHolder[T](val fun: ((T) => Unit))
case class ValueHolder[T](val value: T)

abstract class CopyStrategy[T] extends Function[T,T]

class IdentityCopyStrategy[T] extends CopyStrategy[T] {
    def apply(x:T) : T = { x }
}

//todo clone data
//class CloneCopyStrategy[T <: AnyRef] extends CopyStrategy[T] {
//    def apply(x:T) : T = {
//        val clone: AnyRef = x.clone()
//        clone.asInstanceOf(T)
//    }
//}

//todo reorganize classes
//todo turn strategies into objects
//todo add tests
//todo deploy to GitHub

class ScalaAgent[T](var data: T, val copyStrategy: CopyStrategy[T]) extends Actor {

    def this(data : T) = { this (data, new IdentityCopyStrategy[T])}

    final def act() {
        react {
            case FunctionHolder(fun: (T => T)) => {
                updateData(fun(data))
                act()
            }
            case ProcedureHolder(fun: (T => Unit)) => {
                fun(copyStrategy(data))
                act()
            }
            case ValueHolder(x: T) => {
                updateData(x)
                act()
            }
            case x: Any => {
                //todo handle
                println("Any")
                act()
            }
        }
    }

    private final def updateData(newData: T) {
        data = newData
    }

    final def getValue() : T = {
        val ref: AtomicReference[T] = new AtomicReference[T]()
        val latch: CountDownLatch = new CountDownLatch(1)
        getValue((x:T) => {ref.set(x);latch.countDown})
        latch.await
        return ref.get
    }

    final def getValue(message: (T => Unit)) {
        this ! ProcedureHolder(message)
    }

    final def apply(message: (T => T)) {
        this ! FunctionHolder(message)
    }

    final def apply(message: T) {
        this ! ValueHolder(message)
    }
}

object App {
    def main(args: Array[String]) {
        val agent: ScalaAgent[List[Int]] = new ScalaAgent[List[Int]](List(10))
        agent.start()
        agent(List[Int](1))
        agent((x: List[Int]) => 2 :: x)
        agent((x: List[Int]) => 3 :: x)
        agent((x: List[Int]) => x.reverse)
        agent((x: List[Int]) => {
            x.map(_ * 2)
        })
        agent.getValue((x:List[Int]) => {println("Value: " + x)})
        println("Result: " + agent.getValue())
    }
}
