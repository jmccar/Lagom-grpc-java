import akka.grpc.gen.javadsl.play.{ PlayJavaClientCodeGenerator, PlayJavaServerCodeGenerator }
import sbt.Def
import sbt.Keys.dependencyOverrides

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"


lagomServiceEnableSsl in ThisBuild := true
val `hello-impl-HTTPS-port` = 11000

val lagomGrpcTestkit = "com.lightbend.play" %% "lagom-javadsl-grpc-testkit" % "0.6.0"

lazy val `lagom-java-grpc-example` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`, `hello-proxy-api`, `hello-proxy-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomJava)
  .enablePlugins(AkkaGrpcPlugin) // enables source generation for gRPC
  .enablePlugins(PlayAkkaHttp2Support) // enables serving HTTP/2 and gRPC
  .settings(common)
  .settings(
  akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
  akkaGrpcGeneratedSources :=
    Seq(
      AkkaGrpc.Server,
      AkkaGrpc.Client
    ),
  akkaGrpcExtraGenerators in Compile += PlayJavaServerCodeGenerator,

  // WORKAROUND: Lagom still can't register a service under the gRPC name so we hard-code
  // the port and the use the value to add the entry on the Service Registry
  lagomServiceHttpsPort := `hello-impl-HTTPS-port`,

  libraryDependencies ++= Seq(
    lagomJavadslTestKit,
    lagomLogback,
    lagomJavadslPersistenceCassandra,
    lagomGrpcTestkit
  )
).settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)

lazy val `hello-proxy-api` = (project in file("hello-proxy-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi
    )
  )

lazy val `hello-proxy-impl` = (project in file("hello-proxy-impl"))
  .enablePlugins(LagomJava)
  .enablePlugins(AkkaGrpcPlugin) // enables source generation for gRPC
  .settings(common)
  .settings(
  akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
  akkaGrpcExtraGenerators += PlayJavaClientCodeGenerator,
).settings(
  libraryDependencies ++= Seq(
    lagomJavadslTestKit,
    lagomLogback
  )
)
  .dependsOn(`hello-proxy-api`, `hello-api`)

lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin)

lagomCassandraEnabled in ThisBuild := true
lagomCassandraCleanOnStart in ThisBuild := true
lagomKafkaEnabled in ThisBuild := false

// This adds an entry on the LagomDevMode Service Registry. With this information on
// the Service Registry a client using Service Discovery to Lookup("helloworld.GreeterService")
// will get "https://localhost:11000" and then be able to send a request.
// See declaration and usages of `hello-impl-HTTPS-port`.
lagomUnmanagedServices in ThisBuild := Map("helloworld.GreeterService" -> s"https://localhost:${`hello-impl-HTTPS-port`}")

def common = Seq(
  javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters", "-Werror")
)
