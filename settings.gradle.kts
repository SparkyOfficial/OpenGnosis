rootProject.name = "opengnosis-platform"

// Shared libraries
include("shared:domain")
include("shared:events")
include("shared:common")

// Microservices
include("services:gnosis-iam")
include("services:gnosis-api-gateway")
include("services:gnosis-structure")
include("services:gnosis-scheduler")
include("services:gnosis-journal-command")
include("services:gnosis-analytics-query")
include("services:gnosis-notifier")
