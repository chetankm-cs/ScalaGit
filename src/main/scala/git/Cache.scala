/*
 * Copyright (c) 2014 the original author or authors.
 *
 * Licensed under the MIT License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package git

import scala.concurrent.stm._

case class Cache(
  tags:        Ref[Seq[Tag]]               = Ref(Vector()),
  branches:    Ref[Seq[BaseBranch]]        = Ref(Vector()),
  references:  Ref[Seq[Reference]]         = Ref(Vector()),
  packIndexes: Ref[Option[Seq[PackIndex]]] = Ref(None)
)

object Cache {
  /** Clears the tag cache. */
  def clearTags(repository: Repository) = atomic { implicit txn =>
    repository.cache.tags() = Vector()
  }

  /** Clears the branch cache. */
  def clearBranches(repository: Repository) = atomic { implicit txn =>
    repository.cache.branches() = Vector()
  }

  /** Clears the reference cache. */
  def clearReferences(repository: Repository) = atomic { implicit txn =>
    repository.cache.references() = Vector()
  }

  /** Clears the entire cache. */
  def clear(repository: Repository) {
    clearTags(repository)
    clearBranches(repository)
    clearReferences(repository)
  }

  /** Returns the pack indexes from the cache. */
  private[git] def getPackIndexes(repository: Repository) = atomic { implicit txn =>
    repository.cache.packIndexes()
  }

  /** Caches the pack indexes. */
  private[git] def setPackIndexes(repository: Repository, indexes: Seq[PackIndex]): Seq[PackIndex] = atomic { implicit txn =>
    repository.cache.packIndexes() = Some(indexes)
    indexes
  }

  /** Removes the given tag from the cache. */
  private[git] def deleteTag(repository: Repository, name: String) = atomic { implicit txn =>
    repository.cache.tags() = repository.cache.tags().filterNot(_.name == name).toVector
  }
}