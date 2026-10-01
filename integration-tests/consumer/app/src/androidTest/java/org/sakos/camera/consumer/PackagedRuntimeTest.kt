package org.sakos.camera.consumer

import android.widget.TextView
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackagedRuntimeTest {
    @Test fun minifiedMavenConsumerRunsBundledRuntimeOnSyntheticPixels() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, ConsumerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                assertEquals("Synthetic packaged runtime: OK", (content.getChildAt(0) as TextView).text.toString())
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
