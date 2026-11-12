package com.alarmheaven.alarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alarmheaven.alarm.ui.theme.ChipOff
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * 화면 안에 바로 보이는 휠(iOS 휠 피커와 같은 모양). 가운데 줄이 선택값이고, 놓으면 그 줄로 맞춰진다.
 * [selectedIndex]는 처음 위치로만 쓰고 이후에는 스크롤이 값을 정한다(바깥에서 바꾸지 않는 값에 쓴다).
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleCount: Int = 5,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(state)
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val haptic = LocalHapticFeedback.current
    val latest by rememberUpdatedState(onSelected)

    // 가운데 줄 = 첫 보이는 항목 + (절반 이상 넘어갔으면 1)
    val centered by remember(state) {
        androidx.compose.runtime.derivedStateOf {
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0)
                .coerceIn(0, items.lastIndex)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { centered }.distinctUntilChanged().drop(1).collect {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latest(it)
        }
    }

    Box(modifier.height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).background(ChipOff, RoundedCornerShape(10.dp)))
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items.size) { index ->
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    val isCentered = index == centered
                    Text(
                        items[index],
                        style = appText(if (isCentered) 22 else 19, if (isCentered) FontWeight.ExtraBold else FontWeight.SemiBold,
                            if (isCentered) TextPrimary else TextMuted.copy(alpha = 0.55f)),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
