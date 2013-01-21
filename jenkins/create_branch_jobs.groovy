import jenkins.model.*
import hudson.model.*
import hudson.scm.SubversionSCM.ModuleLocation

/* Poor man's "branch jobs" factory.
 *
 * Assumes there's a view containing disabled template jobs.
 * Assumes those very jobs' names start with "Template_".
 * Assumes you're unlucky enough to have to use subversion.
 *
 * Expects a parameterized job:
 * - VIEW_NAME : the jenkins view containing the template jobs
 * - JOB_GROUP : somewhat related to the branch name
 *               ends up in the jobs' names and display names
 * - SVN_PATH  : the path to the branch in the repository
 *               the template job is already configured
 *               the "SVN_PATH" token is replaced by this value
 */

def env = Thread.currentThread().executable.getEnvVars()

def viewName = env["VIEW_NAME"]
def jobGroup = env["JOB_GROUP"]
def svnPath  = env["SVN_PATH"]

def view = Jenkins.instance.getView(viewName)

for (job in view.items.findAll{item -> item instanceof Job}) {
  def newName = job.name.replaceAll(/^Template_/, "") + "_$jobGroup"
  println "Creating job $newName"

  def newJob = Jenkins.instance.copy(job, newName)
  newJob.displayName = "$jobGroup : $job.displayName"
  for (i=0; i < newJob.scm.locations.length; i++) {
    def location = newJob.scm.locations[i]
    def newRemote = location.remote.replace("SVN_PATH", svnPath)
    newJob.scm.locations[i] = new ModuleLocation(newRemote, location.local)
  }
  newJob.disabled = false
  newJob.save()
}

