package csv
import scala.collection.AbstractIterator


trait ResourceIteratorExtension:
  extension[A,F](it: Iterator[A])

    def close() = ()
    
    def safe(empty: => F)(f: Throwable => F): Iterator[Either[F, A]] = 
       new Iterator[Either[F, A]] {
          override def next() =
             if it.hasNext then ResultCompanion.nonFatal(f)(it.next())
             else Left(empty)
          override def hasNext = it.hasNext 
            }
      
        /** Calls the specified function when the underlying resource is empty. */
    def withClose(f: () => Unit): Iterator[A] =
      new Iterator[A] {
        override def hasNext  = it.hasNext
        override def next() = it.next()
        def close() = {
         it.close()
         f()
        }
    } 

object ResourceIterator {
  val empty: Iterator[Nothing] = new Iterator[Nothing] {
    override def hasNext = false
    override def next() = throw new NoSuchElementException("next on empty resource iterator")
    def close()  = ()
  }
  def apply[A](as: A*): Iterator[A] = as.iterator

  

  
  

}