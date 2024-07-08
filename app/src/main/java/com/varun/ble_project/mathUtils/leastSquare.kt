package com.varun.ble_project.mathUtils

import kotlin.math.*

// Data class for storing optimization results
data class OptimizeResult(
    val x: DoubleArray,
    val cost: Double,
    val funVal: DoubleArray,
    val jac: Array<DoubleArray>,
    val grad: DoubleArray,
    val optimality: Double,
    val activeMask: IntArray,
    val nfev: Int,
    val njev: Int?,
    val status: Int,
    val message: String,
    val success: Boolean
)

val EPS = 2.220446049250313e-16

val TERMINATION_MESSAGES = mapOf(
    -1 to "Improper input parameters status returned from `leastsq`",
    0 to "The maximum number of function evaluations is exceeded.",
    1 to "`gtol` termination condition is satisfied.",
    2 to "`ftol` termination condition is satisfied.",
    3 to "`xtol` termination condition is satisfied.",
    4 to "Both `ftol` and `xtol` termination conditions are satisfied."
)

val FROM_MINPACK_TO_COMMON = mapOf(
    0 to -1, // Improper input parameters from MINPACK.
    1 to 2,
    2 to 3,
    3 to 4,
    4 to 1,
    5 to 0
    // There are 6, 7, 8 for too small tolerance parameters,
    // but we guard against it by checking ftol, xtol, gtol beforehand.
)

fun callMinpack(
    funVal: (DoubleArray) -> DoubleArray,
    x0: DoubleArray,
    jac: ((DoubleArray) -> Array<DoubleArray>)?,
    ftol: Double,
    xtol: Double,
    gtol: Double,
    maxNfev: Int?,
    xScale: Any,
    diffStep: Double?
): OptimizeResult {
    val n = x0.size

    val epsfcn = diffStep?.let { it * it } ?: EPS

    val diag: DoubleArray? = if (xScale is String && xScale == "jac") {
        null
    } else {
        DoubleArray(x0.size) { 1.0 / (xScale as DoubleArray)[it] }
    }

    val fullOutput = true
    val colDeriv = false
    val factor = 100.0

    val (x, info, status) = if (jac == null) {
        val maxNfevFinal = maxNfev ?: 100 * n * (n + 1)
        lmdif(funVal, x0, fullOutput, ftol, xtol, gtol, maxNfevFinal, epsfcn, factor, diag)
    } else {
        val maxNfevFinal = maxNfev ?: 100 * n
        lmder(funVal, jac, x0, fullOutput, colDeriv, ftol, xtol, gtol, maxNfevFinal, factor, diag)
    }

    val f = info["fvec"] as? DoubleArray ?: error("Missing fvec in info")

    val J = jac?.invoke(x) ?: approxDerivative(funVal, x)

    val cost = 0.5 * f.sumByDouble { it * it }
    val g = transpose(J).map { ji -> ji.zip(f).sumOf { (ji, fi) -> ji * fi } }.toDoubleArray()
    val gNorm = g.maxOf {
        abs(it)
    }

    val nfev = info["nfev"] as? Int ?: error("Missing nfev in info")
    val njev = info["njev"] as? Int

    val statusFinal = FROM_MINPACK_TO_COMMON[status] ?: status
    val activeMask = IntArray(x0.size)

    return OptimizeResult(
        x = x,
        cost = cost,
        funVal = f,
        jac = J,
        grad = g,
        optimality = gNorm,
        activeMask = activeMask,
        nfev = nfev,
        njev = njev,
        status = statusFinal,
        message = TERMINATION_MESSAGES[statusFinal] ?: "Unknown termination message",
        success = statusFinal > 0
    )
}

fun prepareBounds(bounds: Pair<DoubleArray, DoubleArray>, n: Int): Pair<DoubleArray, DoubleArray> {
    val (lowerBounds, upperBounds) = bounds
    val lb = lowerBounds.let { if (it.size == 1) DoubleArray(n) { value -> it[0] } else it }
    val ub = upperBounds.let { if (it.size == 1) DoubleArray(n) { value -> it[0] } else it }
    return Pair(lb, ub)
}


fun checkTolerance(
    ftol: Double?,
    xtol: Double?,
    gtol: Double?,
    method: String
): Triple<Double, Double, Double> {
    fun check(tol: Double?, name: String): Double {
        return when {
            tol == null -> 0.0
            tol < EPS -> {
                println("Warning: Setting `$name` below the machine epsilon ($EPS) effectively disables the corresponding termination condition.")
                tol
            }

            else -> tol
        }
    }

    val ftolFinal = check(ftol, "ftol")
    val xtolFinal = check(xtol, "xtol")
    val gtolFinal = check(gtol, "gtol")

    when {
        method == "lm" && (ftolFinal < EPS || xtolFinal < EPS || gtolFinal < EPS) -> {
            throw IllegalArgumentException("All tolerances must be higher than machine epsilon ($EPS) for method 'lm'.")
        }

        ftolFinal < EPS && xtolFinal < EPS && gtolFinal < EPS -> {
            throw IllegalArgumentException("At least one of the tolerances must be higher than machine epsilon ($EPS).")
        }
    }

    return Triple(ftolFinal, xtolFinal, gtolFinal)
}

fun checkXScale(xScale: Any, x0: DoubleArray): DoubleArray {
    return if (xScale is String && xScale == "jac") {
        DoubleArray(x0.size) { 1.0 } // or any default value you want to assign
    } else {
        val xScaleArray = xScale as? DoubleArray
            ?: throw IllegalArgumentException("`x_scale` must be 'jac' or array_like with positive numbers.")
        val valid = xScaleArray.all { it.isFinite() && it > 0 }
        if (!valid) {
            throw IllegalArgumentException("`x_scale` must be 'jac' or array_like with positive numbers.")
        }
        if (xScaleArray.size == 1) {
            DoubleArray(x0.size) { xScaleArray[0] }
        } else if (xScaleArray.size == x0.size) {
            xScaleArray
        } else {
            throw IllegalArgumentException("Inconsistent shapes between `x_scale` and `x0`.")
        }
    }
}


fun checkJacSparsity(jacSparsity: Array<DoubleArray>?, m: Int, n: Int): Array<DoubleArray>? {
    return jacSparsity?.let {
        if (it.size != m || it[0].size != n) {
            throw IllegalArgumentException("`jac_sparsity` has wrong shape.")
        }
        it
    }
}

fun approxDerivative(
    funVal: (DoubleArray) -> DoubleArray,
    x: DoubleArray,
    relStep: Double? = null,
    method: String = "2-point",
    f0: DoubleArray? = null,
    bounds: Pair<DoubleArray, DoubleArray>? = null,
    args: Array<Any>? = null,
    kwargs: Map<String, Any>? = null,
    sparsity: Array<DoubleArray>? = null
): Array<DoubleArray> {
    val m = funVal(x).size
    val n = x.size
    val J = Array(m) { DoubleArray(n) }
    val step = relStep ?: sqrt(EPS)

    for (j in 0 until n) {
        val xStep = x.copyOf()
        xStep[j] += step
        val fStep = funVal(xStep)
        val df = fStep.zip(f0 ?: funVal(x)).map { (fs, f0s) -> (fs - f0s) / step }.toDoubleArray()
        for (i in 0 until m) {
            J[i][j] = df[i]
        }
    }
    return J
}

fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
    return Array(matrix[0].size) { i ->
        DoubleArray(matrix.size) { j ->
            matrix[j][i]
        }
    }
}

fun lmdif(
    funVal: (DoubleArray) -> DoubleArray,
    x0: DoubleArray,
    fullOutput: Boolean,
    ftol: Double,
    xtol: Double,
    gtol: Double,
    maxNfev: Int,
    epsfcn: Double,
    factor: Double,
    diag: DoubleArray?
): Triple<DoubleArray, MutableMap<String, Any>, Int> {
    var x = x0.copyOf()
    val n = x.size
    var nfev = 0
    var cost = funVal(x)
    nfev++

    var v = DoubleArray(cost.size)
    val ipvt = IntArray(n)

    var diagFilled = false
    if (diag != null && diag.size == n) {
        diagFilled = true
        for (i in 0 until n) {
            if (diag[i] <= 0.0) diagFilled = false
        }
    }

    val tol = max(ftol, max(xtol, gtol))
    val wa1 = DoubleArray(n)
    val wa2 = DoubleArray(n)
    val wa3 = DoubleArray(n)

    val info = mutableMapOf<String, Any>()

    var covFac = 100.0 // Adjust as needed

    var iter = 0
    var nfevSuccess = 0
    var jev = 0

    while (true) {
        iter++
        val fjac = Array(n) { DoubleArray(n) }

        for (j in 0 until n) {
            val temp = x[j]
            val h = epsfcn * abs(temp)
            x[j] = temp + h
            val f = funVal(x)
            nfev++
            x[j] = temp
            for (i in cost.indices) {
                fjac[i][j] = (f[i] - cost[i]) / h
            }
        }
        var delta = 0.0
        if (diag != null) {

            if (!diagFilled) {
                for (i in 0 until n) {
                    diag[i] = max(abs(x[i]), 1.0) * sqrt(epsfcn)
                }
            }
            for (i in 0 until n) {
                v[i] = cost[i]
            }
            var qtf = 0.0
            for (j in 0 until n) {
                var sum = 0.0
                for (i in j until n) {
                    sum += fjac[i][j] * v[i]
                }
                val wa1j = sum / diag[j]
                for (i in j until n) {
                    v[i] -= fjac[i][j] * wa1j
                }
                qtf -= wa1j * wa1j
                ipvt[j] = j
            }
            for (j in 0 until n) {
                val l = ipvt[j]
                val tmp = v[l]
                val hl = fjac[j][j] / diag[l]
                for (i in j until n) {
                    v[i] -= hl * tmp
                }
            }
            for (j in 0 until n) {
                wa1[j] = x[j]
                wa2[j] = v[j]
            }

            for (j in 0 until n) {
                val tmp = abs(wa2[j]) * diag[j]
                if (tmp > 0.0) delta = max(delta, tmp)
            }
            if (delta < tol) {
                // Convergence reached
                info["message"] = "Converged successfully."
                break
            }

            if (iter == 1) covFac /= delta

            for (j in 0 until n) {
                wa3[j] = diag[j] * wa2[j]
            }

            var sum2 = 0.0
            for (j in 0 until n) {
                val l = ipvt[j]
                wa3[l] = wa3[j]
                val tmp = wa3[j]
                sum2 += tmp * tmp
            }

            val temp = sqrt(sum2)
            if (iter == 1) qtf = qtf / delta

            for (j in 0 until n) {
                val l = ipvt[j]
                wa3[j] /= temp
                wa2[j] = wa3[j]
            }

            while (true) {
                for (jev in 0 until n) {
                    var j = n - jev - 1
                    val l = ipvt[j]
                    val tmp = wa2[l]
                    for (i in l until n) {
                        wa1[i] -= fjac[i][j] * tmp
                    }
                }

                var sum = 0.0
                for (j in 0 until n) {
                    val l = ipvt[j]
                    val tmp = wa1[l]
                    sum += tmp * tmp
                }

                if (sum > delta) {
                    for (j in 0 until n) {
                        val l = ipvt[j]
                        wa2[l] = 0.0
                        for (i in l until n) {
                            wa2[i] -= fjac[i][j] * wa1[l]
                        }
                    }
                    continue
                }

                break
            }

            for (j in 0 until n) {
                val l = ipvt[j]
                x[l] = wa1[j]
                wa2[j] = diag[l] * wa1[j]
            }

            val tmp = sqrt(sum2 / delta)
            if (tmp <= 0.1 * tol) {
                info["message"] = "Converged successfully (relative stepsize)."
                break
            }

            for (j in 0 until n) {
                val l = ipvt[j]
                wa2[l] = 0.0
                for (i in l until n) {
                    wa2[i] -= fjac[i][j] * wa1[l]
                }
            }

            var sum = 0.0
            for (j in 0 until n) {
                val tmp = wa2[j]
                sum += tmp * tmp
            }

            if (sum <= delta) {
                info["message"] = "Converged successfully (absolute stepsize)."
                break
            }

            if (nfev >= maxNfev) {
                info["message"] = "Maximum number of function evaluations reached."
                break
            }

            val ratio = qtf / sum
            if (ratio <= 0.1) {
                info["message"] = "Converged successfully (sufficient decrease)."
                break
            }

            if (iter == 1) {
                covFac = min(covFac, ratio)
            } else {
                if (ratio >= 0.9999) {
                    break
                }
                if (ratio >= 0.9) {
                    covFac = min(covFac, 10.0)
                } else {
                    covFac = max(covFac, 0.1)
                }
            }

            for (j in 0 until n) {
                val l = ipvt[j]
                wa2[l] = covFac * wa2[j]
            }

            for (j in 0 until n) {
                x[j] -= wa2[j]
            }

            jev = 0
        }
    }

    return Triple(x, info, nfev)
}

fun lmder(
    funVal: (DoubleArray) -> DoubleArray,
    jac: ((DoubleArray) -> Array<DoubleArray>),
    x0: DoubleArray,
    fullOutput: Boolean,
    colDeriv: Boolean,
    ftol: Double,
    xtol: Double,
    gtol: Double,
    maxNfev: Int,
    factor: Double,
    diag: DoubleArray?
): Triple<DoubleArray, MutableMap<String, Any>, Int> {
    var x = x0.copyOf()
    val n = x.size
    var nfev = 0
    var cost = funVal(x)
    nfev++

    val m = cost.size
    val fjac = Array(m) { DoubleArray(n) }
    val v = DoubleArray(m)
    val ipvt = IntArray(n)

    var diagFilled = false
    if (diag != null && diag.size == n) {
        diagFilled = true
        for (i in 0 until n) {
            if (diag[i] <= 0.0) diagFilled = false
        }
    }

    val tol = max(ftol, max(xtol, gtol))
    val wa1 = DoubleArray(n)
    val wa2 = DoubleArray(n)
    val wa3 = DoubleArray(n)

    val info = mutableMapOf<String, Any>()

    var covFac = 100.0 // Adjust as needed

    var iter = 0
    var nfevSuccess = 0
    var jev = 0

    while (true) {
        iter++
        val jacResult = jac(x)
        jacResult.forEachIndexed { index, doubles ->
            System.arraycopy(doubles, 0, fjac[index], 0, n)
        }

        if (!diagFilled) {
            for (i in 0 until n) {
                diag?.let {
                    if (it[i] <= 0.0) {
                        throw IllegalArgumentException("The diagonal elements of the weight matrix must be positive.")
                    }
                }
            }
        }

        for (i in 0 until m) {
            v[i] = cost[i]
        }

        var qtf = 0.0
        for (j in 0 until n) {
            var sum = 0.0
            for (i in j until n) {
                sum += fjac[i][j] * v[i]
            }
            val wa1j = sum / diag!![j]
            for (i in j until n) {
                v[i] -= fjac[i][j] * wa1j
            }
            qtf -= wa1j * wa1j
            ipvt[j] = j
        }

        for (j in 0 until n) {
            val l = ipvt[j]
            val tmp = v[l]
            val hl = fjac[j][j] / diag!![l]
            for (i in j until n) {
                v[i] -= hl * tmp
            }
        }

        for (j in 0 until n) {
            wa1[j] = x[j]
            wa2[j] = v[j]
        }

        var delta = 0.0
        for (j in 0 until n) {
            val tmp = abs(wa2[j]) * diag!![j]
            if (tmp > delta) delta = tmp
        }

        if (delta < tol) {
            info["message"] = "Converged successfully (delta < tol)."
            break
        }

        if (iter == 1) covFac /= delta

        for (j in 0 until n) {
            wa3[j] = diag!![j] * wa2[j]
        }

        var sum2 = 0.0
        for (j in 0 until n) {
            val tmp = wa3[j]
            sum2 += tmp * tmp
        }

        val temp = sqrt(sum2)
        if (iter == 1) qtf /= delta

        for (j in 0 until n) {
            wa3[j] /= temp
            wa2[j] = wa3[j]
        }

        while (true) {
            for (jev in 0 until n) {
                val j = n - jev - 1
                val l = ipvt[j]
                val tmp = wa2[l]
                for (i in l until n) {
                    wa1[i] -= fjac[i][j] * tmp
                }
            }

            var sum = 0.0
            for (j in 0 until n) {
                val l = ipvt[j]
                val tmp = wa1[l]
                sum += tmp * tmp
            }

            if (sum > delta) {
                for (j in 0 until n) {
                    val l = ipvt[j]
                    wa2[l] = 0.0
                    for (i in l until n) {
                        wa2[i] -= fjac[i][j] * wa1[l]
                    }
                }
                continue
            }

            break
        }

        for (j in 0 until n) {
            val l = ipvt[j]
            x[l] = wa1[j]
            wa2[j] = diag!![l] * wa1[j]
        }

        val tmp = sqrt(sum2 / delta)
        if (tmp <= 0.1 * tol) {
            info["message"] = "Converged successfully (relative stepsize)."
            break
        }

        for (j in 0 until n) {
            val l = ipvt[j]
            wa2[l] = 0.0
            for (i in l until n) {
                wa2[i] -= fjac[i][j] * wa1[l]
            }
        }

        var sum = 0.0
        for (j in 0 until n) {
            val tmp = wa2[j]
            sum += tmp * tmp
        }

        if (sum <= delta) {
            info["message"] = "Converged successfully (absolute stepsize)."
            break
        }

        if (nfev >= maxNfev) {
            info["message"] = "Maximum number of function evaluations reached."
            break
        }

        val ratio = qtf / sum
        if (ratio <= 0.1) {
            info["message"] = "Converged successfully (sufficient decrease)."
            break
        }

        if (iter == 1) {
            covFac = min(covFac, ratio)
        } else {
            if (ratio >= 0.9999) {
                break
            }
            if (ratio >= 0.9) {
                covFac = min(covFac, 10.0)
            } else {
                covFac = max(covFac, 0.1)
            }
        }

        for (j in 0 until n) {
            val l = ipvt[j]
            wa2[l] = covFac * wa2[j]
        }

        for (j in 0 until n) {
            x[j] -= wa2[j]
        }

        jev = 0
    }

    return Triple(x, info, nfev)
}


// Trilateration Functions

fun trilaterationEquations(
    coordinates: DoubleArray,
    beaconLocs: List<List<Double>>,
    distances: List<Double>
): DoubleArray {
    val (x, y, z) = coordinates
    val errors = DoubleArray(beaconLocs.size)

    for (i in beaconLocs.indices) {
        val (beaconX, beaconY, beaconZ) = beaconLocs[i]
        val distance = distances[i]
        val error =
            (x - beaconX).pow(2) + (y - beaconY).pow(2) + (z - beaconZ).pow(2) - distance.pow(2)
        errors[i] = error
    }

    return errors
}

// Trilateration calculation function
fun main() {
    val beaconLocations = listOf(
        listOf(2.0, 4.0, 1.0),
        listOf(8.0, 2.0, 3.0),
        listOf(5.0, 7.0, 5.0)
    )
    val estimatedDistances = listOf(6.0, 4.0, 7.0)
    val initialGuess = doubleArrayOf(5.0, 5.0, 5.0)

    val funVal: (DoubleArray) -> DoubleArray =
        { coordinates -> trilaterationEquations(coordinates, beaconLocations, estimatedDistances) }

    val solution = callMinpack(funVal, initialGuess, null, 1e-8, 1e-8, 1e-8, null, "jac", null)
    val (optimizedX, optimizedY, optimizedZ) = solution.x

    println("Optimized Coordinates: x=$optimizedX, y=$optimizedY, z=$optimizedZ")
}
