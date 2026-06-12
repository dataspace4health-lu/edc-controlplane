rootProject.name = "controlplane"

include("extensions")
include("extensions:scope")
include("extensions:common")
include("extensions:common:http")
include("extensions:common:http:client")
include("extensions:common:decentralized-claims-issuers-configuration")
include("extensions:policy")
include("extensions:s3")

findProject(":extensions:scope")?.name = "scope"
findProject(":extensions:policy")?.name = "policy"
findProject(":extensions:s3")?.name = "s3"
findProject(":extensions:common:http:client")?.name = "client"
findProject(":extensions:common:decentralized-claims-issuers-configuration")?.name = "decentralized-claims-issuers-configuration"