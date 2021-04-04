val zioGrpcVersion = "0.5.0"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.2")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
addSbtPlugin("org.scalameta"                            % "sbt-scalafmt"     % "2.4.2")
