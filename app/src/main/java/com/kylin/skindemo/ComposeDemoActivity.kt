package com.kylin.skindemo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylin.skindemo.compose.SkinTheme
import com.kylin.skindemo.compose.skinColorScheme
import com.netease.skin.library.base.SkinActivity

/**
 * Compose 组件换肤案例页（完整组件示例 + 状态保持验证）。
 *
 * 继承 [SkinActivity] 获得 `getResources()` 皮肤感知覆盖（[com.kylin.skinlibrary.SkinnableResources]），
 * 因此页面里**全部使用 Compose 系统资源方法**：`colorResource` / `painterResource` /
 * `stringResource` / `dimensionResource`，不引入任何自定义 wrapper——这些系统方法内部都是
 * `LocalContext.current.resources.getXxx(id)`，切肤时 `SkinnableResources` 自动按皮肤包同名
 * 资源取值，业务代码保持原生 Compose 写法不变。
 *
 * 重组由 [SkinTheme]（内部是 SkinComposeProvider）负责：它通过 staticCompositionLocalOf 的
 * [com.kylin.skinlibrary.compose.LocalSkinVersion] 在皮肤切换时重建 provider 子树，使所有
 * 资源读点重新执行、拿到新皮肤色，而 `remember { mutableStateOf(...) }` 里的页面状态
 * （输入框文本、开关勾选、复选框、单选、滑块值、计数）保持不变。
 */
class ComposeDemoActivity : SkinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkinTheme {
                // 原生主题适配：把皮肤色映射到 ColorScheme，组件自动取色，业务代码不逐个传 colors
                MaterialTheme(colorScheme = skinColorScheme()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
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
        // ===== 文本：颜色随皮肤切换，字符串内容保持稳定 =====
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

        // ===== 图标换肤：painterResource 同名 drawable 映射（蓝 ↔ 橙） =====
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

        // ===== 卡片：底色 + 尺寸（dimen）+ 按钮 =====
        // Card / Button 不传 colors，自动从 MaterialTheme.colorScheme（皮肤感知）取色
        Card(modifier = Modifier.fillMaxWidth()) {
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
                // 尺寸换肤：皮肤包覆盖同名 dimens（8dp ↔ 16dp），间距随主题变化
                Spacer(Modifier.height(dimensionResource(R.dimen.skin_demo_gap)))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.compose_button))
                }
            }
        }

        // ===== 有状态控件：切主题后勾选/开关/单选状态保持不变 =====
        // Switch / Checkbox / RadioButton / Slider 均不传 colors，自动从 ColorScheme 取色
        var switchOn by remember { mutableStateOf(true) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = switchOn, onCheckedChange = { switchOn = it })
            Text(
                text = stringResource(R.string.compose_switch),
                modifier = Modifier.padding(start = 8.dp),
                color = colorResource(R.color.text_primary)
            )
        }

        var cb by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cb, onCheckedChange = { cb = it })
            Text(
                text = stringResource(R.string.compose_checkbox),
                modifier = Modifier.padding(start = 8.dp),
                color = colorResource(R.color.text_primary)
            )
        }

        var radio by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = radio, onClick = { radio = !radio })
            Text(
                text = stringResource(R.string.compose_radio),
                modifier = Modifier.padding(start = 8.dp),
                color = colorResource(R.color.text_primary)
            )
        }

        // ===== 滑块 + 计数器（状态保持） =====
        var slider by remember { mutableStateOf(0.5f) }
        Text(
            text = stringResource(R.string.compose_slider),
            color = colorResource(R.color.text_primary)
        )
        Slider(value = slider, onValueChange = { slider = it })

        var count by remember { mutableStateOf(0) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { count++ }) { Text("+") }
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 18.sp,
                color = colorResource(R.color.text_primary)
            )
            Button(onClick = { count-- }) { Text("-") }
        }

        // ===== 输入框：切换主题后已输入文本保持不变（本次优化的核心验证点） =====
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.compose_textfield)) }
        )

        // ===== 字符串数组标签（内容保持稳定） + 强调色块（main_style 皮肤色） =====
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("标签A", "标签B", "标签C").forEach { tag ->
                Box(
                    modifier = Modifier
                        .background(color = colorResource(R.color.main_style), shape = CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(tag, fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // ===== 强调色块：main_style 皮肤色 =====
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
