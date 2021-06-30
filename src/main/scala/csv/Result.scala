package csv
import scala.util.{Failure, Success, Try}


object ResultCompanion:
   /** Evaluates the specified expression, catching non-fatal errors and sticking them in a `Left`. */
   def nonFatal[E, S](f: Throwable => E)(s: => S): Either[E, S] = Try(s).toEither.left.map(f)

     /** Provides companion object methods for result types that do not have a sane default error type.
    *
    * If your specialised result type has a sane default (such as `TypeError` for `DecodeResult` in kantan.csv), use
    * [[WithDefault]] instead.
    */
   trait Simple[F]:
   /** Turns the specified value into a success. */
    inline def success[S](s: S): Either[F, S] = Right(s)

    /** Turns the specified value into a failure. */
    inline def failure(f: F): Either[F, Nothing] = Left(f)
 

   trait WithDefault[F] extends Simple[F]:
 
    /** Turns an exception into an error. */
     protected def fromThrowable(t: Throwable): F

    /** Attempts to evaluate the specified expression. */
     inline def apply[S](s: => S): Either[F, S] = nonFatal(fromThrowable)(s)

    /** Turns the specified `Try` into a result. */
     inline def fromTry[S](t: Try[S]): Either[F, S] = 
      t match 
        case Success(s) => Right(s)
        case Failure(e) => Left(fromThrowable(e))

     /** Similar to [[WithDefault]], but uses [[error.IsError IsError]] to deal with error cases. */
   abstract class WithError[F: IsError] extends WithDefault[F]:
    override def fromThrowable(t: Throwable) = IsError[F].fromThrowable(t)

  
    
