/*
 * Selectively extracted from the user-authorized camera/gallery application source
 * at historical source revision omitted. See docs/EXTRACTION_MANIFEST.md.
 */
package org.sakos.camera.safety.opennsfw2

import android.content.Context
import org.json.JSONObject

data class IntegratedStillGatePolicyConstants(
    val highTierThreshold: Float,
    val rawNsfwBlockFloor: Float,
    val elevatedDetectionFloor: Float,
    val supportiveDetectionFloor: Float,
    val corroboratedDetectionAnchorFloor: Float,
    val extremeDetectionFloor: Float,
    val stage1EasyAllowMax: Float,
    val stage1AllowMax: Float,
    val stage2AllowMax: Float,
    val stage2CorroboratedDetectionFloor: Float,
    val nearFloorLateralGap: Float,
)

object IntegratedStillGatePolicyDefaults {
    private const val assetPath = "policy/opennsfw2_still_gate_policy.json"

    val fallback: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyConstants(
        highTierThreshold = 0.75f,
        rawNsfwBlockFloor = 0.45f,
        elevatedDetectionFloor = 0.24f,
        supportiveDetectionFloor = 0.30f,
        corroboratedDetectionAnchorFloor = 0.45f,
        extremeDetectionFloor = 0.85f,
        stage1EasyAllowMax = 0.12f,
        stage1AllowMax = 0.20f,
        stage2AllowMax = 0.30f,
        stage2CorroboratedDetectionFloor = 0.25f,
        nearFloorLateralGap = 0.03f,
    )

    fun load(context: Context): IntegratedStillGatePolicyConstants = runCatching {
        context.assets.open(assetPath).use { inputStream ->
            fromJson(JSONObject(inputStream.bufferedReader(Charsets.UTF_8).readText()))
        }
    }.getOrElse {
        fallback
    }

    internal fun fromJson(json: JSONObject): IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyConstants(
        highTierThreshold = json.optDouble("highTierThreshold", fallback.highTierThreshold.toDouble()).toFloat(),
        rawNsfwBlockFloor = json.optDouble("rawNsfwBlockFloor", fallback.rawNsfwBlockFloor.toDouble()).toFloat(),
        elevatedDetectionFloor = json.optDouble("elevatedDetectionFloor", fallback.elevatedDetectionFloor.toDouble()).toFloat(),
        supportiveDetectionFloor = json.optDouble("supportiveDetectionFloor", fallback.supportiveDetectionFloor.toDouble()).toFloat(),
        corroboratedDetectionAnchorFloor = json.optDouble(
            "corroboratedDetectionAnchorFloor",
            fallback.corroboratedDetectionAnchorFloor.toDouble(),
        ).toFloat(),
        extremeDetectionFloor = json.optDouble("extremeDetectionFloor", fallback.extremeDetectionFloor.toDouble()).toFloat(),
        stage1EasyAllowMax = json.optDouble(
            "stage1EasyAllowMax",
            fallback.stage1EasyAllowMax.toDouble(),
        ).toFloat(),
        stage1AllowMax = json.optDouble("stage1AllowMax", fallback.stage1AllowMax.toDouble()).toFloat(),
        stage2AllowMax = json.optDouble("stage2AllowMax", fallback.stage2AllowMax.toDouble()).toFloat(),
        stage2CorroboratedDetectionFloor = json.optDouble(
            "stage2CorroboratedDetectionFloor",
            fallback.stage2CorroboratedDetectionFloor.toDouble(),
        ).toFloat(),
        nearFloorLateralGap = json.optDouble(
            "nearFloorLateralGap",
            fallback.nearFloorLateralGap.toDouble(),
        ).toFloat(),
    )
}


