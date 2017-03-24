package controllers.gmql

import javax.inject.Singleton

import controllers.gmql.ResultUtils.NA
import io.swagger.annotations.{ApiImplicitParams, ApiOperation, _}
import it.polimi.genomics.core.{BinSize, GMQLSchemaFormat, GMQLScript, ImplementationPlatform}
import it.polimi.genomics.manager.Exceptions.{InvalidGMQLJobException, NoJobsFoundException}
import it.polimi.genomics.manager.Launchers.GMQLSparkLauncher
import it.polimi.genomics.manager.{GMQLContext, GMQLExecute, GMQLJob}
import org.apache.spark.SparkContext
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConversions._


/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
@Api(value = SwaggerUtils.swaggerQueryManager, produces = "application/json, application/xml")
class QueryMan extends Controller {

  import utils.GmqlGlobal._

  @ApiOperation(value = "Execute the query",
    notes = "Execute query and for the result user needs to check trace the job.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(
    name = "body",
    dataType = "string", paramType = "body"
    //    , examples = new Example(Array(new ExampleProperty(value = "{\n\t\"schema_file\": \"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/HG19_ANN.schema\",\n\t\"data_files\": [\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed.meta\"\n\t]\n}")))
  ),new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def runQuery(queryName: String,
               @ApiParam(allowableValues = "tab, gtf") outputType: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val outputFormat = GMQLSchemaFormat.getType(outputType)

    val queryOption = request.body.asText


    lazy val queryResult: QueryResult = queryOption match {
      case None =>
        Logger.error("Query must be send in the request body" + "\n" + username + " : " + queryName + "\n" + queryOption + "\n\n\n\n\n")
        ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        QueryResult(None)
      case Some(query) =>
        Logger.info("\n" + username + " : " + queryName + "\n" + queryOption + "\n\n\n\n\n")
        val server = GMQLExecute()
        val job = registerJob(username, query, queryName, outputFormat)
        server.execute(job.jobId, new GMQLSparkLauncher(job))
        QueryResult(Some(Job(job.jobId, Some(job.getJobStatus.toString))))
    }

    render {
      case Accepts.Xml() =>
        if (queryResult.job.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(scala.xml.Utility.trim(queryResult.getXml))
      case Accepts.Json() =>
        if (queryResult.job.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(Json.toJson(queryResult))
      case _ => NA
    }
  }


  private def registerJob(username: String, query: String, queryName: String, outputFormat: GMQLSchemaFormat.Value) = {
    val server = GMQLExecute()
    val gmqlScript = new GMQLScript(query, queryName)
    val binSize = new BinSize(5000, 5000, 1000)
    val emptyContext: SparkContext = null
    val gmqlContext = new GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, binSize, username, emptyContext)
    server.registerJob(gmqlScript, gmqlContext, "")
  }

  @ApiOperation(value = "Compile query",
    notes = "Compile query and for the result user needs to check trace the job.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(
    name = "body",
    dataType = "string", paramType = "body"
    //    , examples = new Example(Array(new ExampleProperty(value = "{\n\t\"schema_file\": \"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/HG19_ANN.schema\",\n\t\"data_files\": [\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed.meta\"\n\t]\n}")))
  ),new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def compileQuery() = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val queryOption = request.body.asText
    val outputFormat = GMQLSchemaFormat.TAB
    val queryName = "only_compile"

    lazy val queryResult: QueryResult = queryOption match {
      case None =>
        Logger.error("Query must be send in the request body" + "\n" + username + " : " + queryName + "\n" + queryOption + "\n\n\n\n\n")
        ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        QueryResult(None)
      case Some(query) =>
//        val gmqlScript = new GMQLScript(query, queryName)
//        val binSize = new BinSize(5000, 5000, 1000)
//        val emptyContext: SparkContext = null
//        val gmqlContext = new GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, binSize, username, emptyContext)
//        val job: GMQLJob = new GMQLJob(gmqlContext,gmqlScript,gmqlContext.username)
//        job.compile()
        val job = registerJob(username, query, queryName, outputFormat)
        QueryResult(Some(Job(job.jobId, Some(job.getJobStatus.toString), Some(job.jobOutputMessages.toString()))))
    }

    render {
      case Accepts.Xml() =>
        if (queryResult.job.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(scala.xml.Utility.trim(queryResult.getXml))
      case Accepts.Json() =>
        if (queryResult.job.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(Json.toJson(queryResult))
      case _ => NA
    }
  }

  @ApiOperation(value = "Get the jobs", notes = "Get the list of the jobs of the current user")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getJobs = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")

    lazy val jobs: JobList = {
      try {
        JobList(GMQLExecute().getUserJobs(username).map(Job(_)))
      } catch {
        case _: NoJobsFoundException => JobList(List.empty)
      }
    }

    render {
      case Accepts.Xml() => Ok(jobs.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobs))
      case _ => NA
    }
  }

  @ApiOperation(value = "Trace the job", notes = "Trace the job with the id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def traceJob(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")

    lazy val server = GMQLExecute();
    //        String user = sc.getUserPrincipal().getName();
    lazy val job = server.getGMQLJob(username, jobId)

    lazy val datasets = server.getJobDatasets(jobId).map(Dataset(_))
    lazy val jobResult = Job(job.jobId, Some(job.status.toString), Some(job.getMessage()), Some(datasets), Some(job.getExecutionTime()))

    render {
      case Accepts.Xml() => Ok(jobResult.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobResult))
      case _ => NA
    }

    //status
    //message
    //ds names
    //execution time


    //      var elapsed =/* "Compilation Time: "+job.getCompileTime()+"\n"+*/"Execution Time: "+job.getExecutionTime()/*+"\nCreate Result DataSet Time: "+job.getDSCreationTime()*/;
    //
    //      if(elapsed ==null)
    //      {
    //        System.out.println("\n\nNo exec Time preduced yet\n\n");
    //        elapsed="";
    //      }
    //
    //      lazy val datasets = (for (ds: IRDataSet <- GMQL_Globals().repository.listAllDSs(username)) yield Dataset(ds.position, Some(username))) ;
    //
    //
    //      List<String> datasets;
    //      String DSnames="";
    //      StringBuilder Datasetsnames = new StringBuilder();
    //      List<String> DSs = new LinkedList<String>();
    //      try {
    //        DSs = server.getJobDatasets(jobId);
    //        if (!(datasets = server.getJobDatasets(jobId)).isEmpty()) {
    //          for (String ds : datasets) {
    //            Datasetsnames.append("," + ds);
    //          }
    //          try {
    //            DSnames = Datasetsnames.toString().substring(1, Datasetsnames.toString().length());
    //          } catch (Exception ex) {
    //            System.out.println("There is no result to show " + ex.getMessage());
    //          }
    //        }
    //      }catch (Exception ex){
    //        System.out.println (ex.getMessage());
    //      }
    //
    //      System.out.println("Datasets: "+DSnames);
    //      GMQLJobStatusXML jobStateXml = new GMQLJobStatusXML(
    //        new Date(),
    //        job.getJobStatus().toString(),
    //        job.getMessage(),
    //        DSs,
    //        DSnames,
    //        elapsed
    //      );
    //
    //      return Response.ok(jobStateXml).build();
    //
    //
    //
    //      val response = new QueryManager().traceJobv2(username, jobId)
    //      //    val resAsString = ResultUtils.unMarshallClass(response, classOf[GMQLJobStatusXML], false)
    //      //    Ok(resAsString).as("text/xml")
    //      ResultUtils.renderJaxb(response)
  }

  @ApiOperation(value = "Get the log of the job", notes = "Returns the log of the job with job id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getLog(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    lazy val jobLog = Log(GMQLExecute.getJobLog(username, jobId))

    render {
      case Accepts.Xml() => Ok(jobLog.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobLog))
      case _ => NA
    }
  }


  /**
    * In order to stop the job this service should be called.
    *
    * @param jobId the id of the job.
    * @return Ok(HTTP 200) with a message that stops the job if the stop execution done correctly,
    *         Forbidden(HTTP 403) message if otherwise(if the job is not exists or the job id is not related to the user)
    */
  @ApiOperation(value = "Stop the job", notes = "Stops the job with the jobs id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
    def stopJob(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val server = GMQLExecute()
    try {
      val job: GMQLJob = server.getGMQLJob(username, jobId)
      job.submitHandle.killJob()
      Ok("Job is stopping.")
    } catch {
      case e: InvalidGMQLJobException => Forbidden(e.getMessage)
    }
  }


}
