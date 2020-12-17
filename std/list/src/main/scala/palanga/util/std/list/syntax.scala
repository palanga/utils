package palanga.util.std.list

object syntax {

  implicit class ListOps[T](val self: List[Option[T]]) extends AnyVal {
    def sequence: Option[List[T]] = self match {
      case Nil    => Some(Nil)
      case h :: t => h flatMap (r => t.sequence map (r :: _))
    }
  }

}
