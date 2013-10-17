package git

import java.util.Date
import git.util.Parser._
import scala.collection.mutable.ListBuffer

case class Commit(
  override val id: ObjectId,
  override val header: ObjectHeader,
  override val repository: Repository,
  authorName: String,
  authorEmail: String,
  authorDate: Date,
  committerName: String,
  committerEmail: String,
  commitDate: Date,
  message: String,
  treeId: ObjectId,
  parentIds: List[ObjectId]
) extends Object {
  def tree(): Tree = repository.database.findObjectById(treeId).get.asInstanceOf[Tree]

  def parents(): List[Commit] = Nil // TODO: Implement.
}

object Commit {
  def fromObjectFile(bytes: Array[Short], repository: Repository, id: ObjectId, header: Option[ObjectHeader]): Commit = {
    /*
      Example structure:

	    "tree" <SP> <HEX_OBJ_ID> <LF>
		  ( "parent" <SP> <HEX_OBJ_ID> <LF> )*
		  "author" <SP>
  		  <SAFE_NAME> <SP>
  		  <LT> <SAFE_EMAIL> <GT> <SP>
  		  <GIT_DATE> <LF>
  		"committer" <SP>
  		  <SAFE_NAME> <SP>
  	    <LT> <SAFE_EMAIL> <GT> <SP>
  		  <GIT_DATE> <LF>
  		<LF>
  		<DATA>
    */

    // The object file starts with "tree ", let's skip that.
    var data = bytes.drop(5)

    // Followed by tree hash.
    val treeId = ObjectId.fromHash(new String(data.take(40)))

    data = data.drop(40 + 1) // One LF.

    val parentIdsBuffer = new ListBuffer[ObjectId]

    // What follows is 0-n number of parent references.
    def parseParentIds() {
      // Stop if the data does not begin with "parent".
      if (new String(data.takeWhile(_ != 32)) == "parent") {
        data = data.drop(7) // Skip "parent ".

        parentIdsBuffer += ObjectId.fromHash(new String(data.take(40)))

        data = data.drop(40 + 1) // One LF.

        parseParentIds()
      }
    }

    val parentIds = parentIdsBuffer.toList

    parseParentIds()

    data = data.drop(7) // Skip the "author " data.

    val authorData = parseUserFields(data)
    val authorName = authorData._1
    val authorEmail = authorData._2
    val authorDate = authorData._3
    data = authorData._4

    data = data.drop(10) // Skip the "committer " data.

    val committerData = parseUserFields(data)
    val committerName = committerData._1
    val committerEmail = committerData._2
    val commitDate = committerData._3
    data = committerData._4

    // Finally the commit message.
    val message = new String(data).trim

    Commit(
      id = id,
      header = header match {
        case Some(v) => v
        case None => ObjectHeader(ObjectType.Commit)
      },
      repository = repository,
      authorName = authorName,
      authorEmail = authorEmail,
      authorDate = authorDate,
      committerName = committerName,
      committerEmail = committerEmail,
      commitDate = commitDate,
      message = message,
      treeId = treeId,
      parentIds = parentIds
    )
  }
}