package git

import scala.collection._
import scala.collection.generic._
import scala.collection.mutable.ListBuffer

class CommitLog(val repository: Repository) extends Traversable[Commit] {
  def find(filter: CommitFilter): List[Commit] = {
    var buffer = new ListBuffer[Commit]

    // Prepare the "since" value. Default to HEAD.
    val since: List[AnyRef] = if (filter.since == null) List(repository.head.tip().id) else filter.since

    val sinceIds = since.map{
      case a: ObjectId => a
      case b: Branch => b.tip().id
      case _ => throw new Exception("Invalid commit filter: does the repository HEAD exist?")
    }

    if (filter.sort == CommitSortStrategy.Time) {
      // Fill buffer with commits from all "since" sources.
      sinceIds.foreach((sinceId: ObjectId) => {
        def findNSinceId(n: Int, id: ObjectId) {
          repository.database.findObjectById(id).get match { // TODO: Handle when this ends (None).
            case commit: Commit => {
              if (commit.id != null && !buffer.contains(commit)) buffer += commit
              if (n > 1) commit.parentIds.foreach(findNSinceId(n -1, _))
            }
          }
        }

        findNSinceId(filter.limit, sinceId)
      })

      buffer = buffer.sortBy(_.commitDate).take(filter.limit)
    }

    buffer.toList
  }

  def foreach[T](f: Commit => T): Unit = {

  }
}