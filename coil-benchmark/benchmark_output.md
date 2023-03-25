Benchmarked on a Pixel 3 running Android 12.

## coil-sample-view

| BaselineProfileBenchmark_fullCompilation |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 330.8  | median 340.1 | max 371.7  |            |
| frameDurationCpuMs                       | P50   14.2 | P90   41.2   | P95  145.5 | P99  149.6 |
| frameOverrunMs                           | P50    5.0 | P90  147.4   | P95  188.9 | P99  198.3 |

| BaselineProfileBenchmark_noneCompilation |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 303.3  | median 314.4 | max 334.7  |            |
| frameDurationCpuMs                       | P50   14.0 | P90   77.3   | P95  147.3 | P99  150.4 |
| frameOverrunMs                           | P50    4.5 | P90  151.3   | P95  192.0 | P99  200.8 |

| BaselineProfileBenchmark_baselineProfile |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 304.2  | median 309.5 | max 367.7  |            |
| frameDurationCpuMs                       | P50   12.4 | P90   54.7   | P95  149.1 | P99  172.1 |
| frameOverrunMs                           | P50    4.2 | P90  150.5   | P95  187.8 | P99  209.6 |

## coil-sample-compose

| BaselineProfileBenchmark_fullCompilation |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 350.7  | median 361.4 | max 379.6  |            |
| frameDurationCpuMs                       | P50   12.2 | P90   71.2   | P95  195.7 | P99  202.1 |
| frameOverrunMs                           | P50   -5.1 | P90  177.1   | P95  204.2 | P99  216.2 |

| BaselineProfileBenchmark_noneCompilation |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 378.5  | median 404.9 | max 516.4  |            |
| frameDurationCpuMs                       | P50   27.3 | P90  362.9   | P95  365.9 | P99  369.4 |
| frameOverrunMs                           | P50  279.5 | P90  375.2   | P95  379.8 | P99  382.6 |

| BaselineProfileBenchmark_baselineProfile |            |              |            |            |
|------------------------------------------|------------|--------------|------------|------------|
| timeToInitialDisplayMs                   | min 362.2  | median 379.5 | max 467.5  |            |
| frameDurationCpuMs                       | P50   12.3 | P90  115.0   | P95  209.7 | P99  255.6 |
| frameOverrunMs                           | P50    3.7 | P90  220.9   | P95  228.2 | P99  271.9 |
