package com.kylin.skindemo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylin.skindemo.compose.SkinTheme
import com.netease.skin.library.base.SkinActivity

/**
 * Compose 换肤案例页。
 *
 * 继承 [SkinActivity] 获得 `getResources()` 皮肤感知覆盖，使 `colorResource` / `stringResource`
 * 等 Compose 资源 API 自动按当前皮肤取值；[SkinTheme] 负责在皮肤切换时触发重组。
 * 切主题后本页所有用资源 ID 取色的控件无需手动刷新即可跟随变化。
 */
class ComposeDemoActivity : SkinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkinTheme {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorResource(R.color.dialog_bg)
                    ) {
                        ComposeDemoScreen()
                    }
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 全局主题切换悬浮按钮：Compose 页同样可切肤看效果
        ThemeSwitcher.installFab(this)
    }
}

@Composable
private fun ComposeDemoScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.compose_demo_title),
            fontSize = 22.sp,
            color = colorResource(R.color.text_primary)
        )
        Text(
            text = stringResource(R.string.compose_demo_desc),
            fontSize = 14.sp,
            color = colorResource(R.color.text_secondary)
        )

        // 图标换肤：painterResource 同名 drawable 映射
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_skin_demo),
                contentDescription = "skin icon",
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.compose_icon),
                modifier = Modifier.padding(start = 12.dp),
                color = colorResource(R.color.text_primary)
            )
        }

        // 卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.compose_card_title),
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_primary)
                )
                Text(
                    text = stringResource(R.string.compose_card_body),
                    fontSize = 13.sp,
                    color = colorResource(R.color.text_hint)
                )
            }
        }

        // 按钮
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.compose_button))
        }

        // 开关 + 复选框
        Row(verticalAlignment = Alignment.CenterVertically) {
            var checked by remember { mutableStateOf(true) }
            Switch(checked = checked, onCheckedChange = { checked = it })
            Spacer(Modifier.height(0.dp))
            Text(
                text = stringResource(R.string.compose_switch),
                modifier = Modifier.padding(start = 8.dp),
                color = colorResource(R.color.text_primary)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            var cb by remember { mutableStateOf(false) }
            Checkbox(checked = cb, onCheckedChange = { cb = it })
            Text(
                text = stringResource(R.string.compose_checkbox),
                modifier = Modifier.padding(start = 8.dp),
                color = colorResource(R.color.text_primary)
            )
        }

        // 滑块
        var slider by remember { mutableStateOf(0.5f) }
        Text(
            text = stringResource(R.string.compose_slider),
            color = colorResource(R.color.text_primary)
        )
        Slider(value = slider, onValueChange = { slider = it })

        // 输入框
        var text by remember { mutableStateOf("") }
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.compose_textfield)) }
        )

        // 强调色块
        Text(
            text = stringResource(R.string.compose_accent),
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.main_style))
                .padding(16.dp),
            color = colorResource(R.color.text_primary)
        )
    }
}
