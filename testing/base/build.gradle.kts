val vs: Versions = versions()

dependencies {

    api(project(":core"))
    api("org.scala-lang:scala-reflect:${vs.scalaV}")
//    api("com.lihaoyi:utest_${vs.scalaBinaryV}:0.7.10")
}

