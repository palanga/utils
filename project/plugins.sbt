val zioGrpcVersion = "0.4.3"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.1")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
addSbtPlugin("org.scalameta"                            % "sbt-scalafmt"     % "2.4.2")
