/*-------------------------------------------------------------------------
 *
 *
 * Copyright (c) 2024, Myron Scott  <myron@weaverdb.org>
 *
 * All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 *
 *-------------------------------------------------------------------------
 */
package org.weaverdb.myweaverdbexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import org.weaverdb.DBReference
import org.weaverdb.ExecutionException
import org.weaverdb.FetchSet
import org.weaverdb.android.DBHome
import org.weaverdb.myweaverdbexample.ui.theme.MyWeaverDBExampleTheme
import java.util.Date
import kotlin.streams.asSequence

data class Click(val x: Int, val y: Int, val moment: Date)


class MainActivity : ComponentActivity() {

    private var clicks: Long = 0
    private val history: MutableList<Click> = mutableListOf()
    private var dbref: DBReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        DBHome.startInstance(applicationContext.filesDir.toPath())
        if (!DBHome.dbExists("uitest")) {
            val ref = DBHome.connect("uitest")
            ref.execute("create table clickcounter (x int4, y int4, moment timestamp)")
            dbref = ref
        } else {
            dbref = DBHome.connect("uitest")
        }

        val list = FetchSet.builder(dbref).parse("select x,y,moment from clickcounter order by moment")
            .output<Int>(1, Int::class.java)
            .output<Int>(2, Int::class.java)
            .output<Date>(3, Date::class.java)
            .execute().map { r ->
                Click(
                    x = (r[0].get() as Int),
                    y = (r[1].get() as Int),
                    moment = (r[2].get() as Date)
                )
            }.asSequence()

        history += list
        clicks = history.size.toLong()
        setContent {
            MyWeaverDBExampleTheme {
                var activeClicks by rememberSaveable { mutableStateOf<Long?>(0L) }
                activeClicks = clicks
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            activeClicks = insertClick(offset.x, offset.y)
                        }
                    }
                ) { innerPadding ->
                    if (activeClicks != null) {
                        Column (
                            modifier = Modifier.padding(innerPadding)
                        ){
                            Greeting(
                                clicks = activeClicks,
                                name = "WeaverDB",
                                onClick = {
                                    clicks = 0
                                    activeClicks = clicks
                                    dbref?.execute("delete from clickcounter")
                                    history.clear()
                                }
                            )
                            History(
                                clicks = history
                            )
                        }
                    }
                }
            }
        }
    }

    private fun insertClick(x: Float, y: Float): Long {
        try {
            FetchSet.builder(dbref)
                .parse("insert into clickcounter (x,y,moment) values (:x,:y,:time)")
                .input<Int>("x", x.toInt())
                .input<Int>("y", y.toInt())
                .input<Date>("time", Date()).execute().close()
            history += Click(x = x.toInt(), y = y.toInt(), moment = Date())
            clicks += 1
        } catch (ee: ExecutionException) {
            Log.e("EXAMPLE", "an error accessing database", ee)
        }
        return clicks
    }

    override fun onDestroy() {
        super.onDestroy()
        dbref?.close()
    }
}

@Composable
fun Greeting(clicks: Long?,name: String, onClick : ()->Unit, modifier: Modifier = Modifier) {
        Text(
            text = "clicks: $clicks on $name",
            modifier = modifier
        )
        Button(
            onClick = {
                onClick()
            }
        ) {
            Text(text = "reset")
        }
}

@Composable
fun History(clicks: List<Click>) {
    LazyColumn {
        items(clicks) { click ->
            ClickRecord(click.x, click.y, click.moment)
        }
    }
}
@Composable
fun ClickRecord(x: Int, y: Int, moment: Date) {
    Text("x=$x, y=$y, moment=$moment")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyWeaverDBExampleTheme {
        Greeting(0,"Android", {})
    }
}