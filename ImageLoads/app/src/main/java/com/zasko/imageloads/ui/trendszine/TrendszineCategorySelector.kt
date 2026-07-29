package com.zasko.imageloads.ui.trendszine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R

@Composable
fun TrendszineTitleCategorySelector(
    categories: List<TrendszineCategory>,
    selectedParentCategory: TrendszineCategory,
    selectedCategory: TrendszineCategory,
    onParentCategorySelected: (TrendszineCategory) -> Unit,
    onChildCategorySelected: (TrendszineCategory) -> Unit,
) {
    var expandedParentUrl by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(categories, key = { it.url }) { category ->
            val selected = category.url == selectedParentCategory.url
            val expanded = expandedParentUrl == category.url
            Box {
                TrendszineCategoryChip(
                    text = category.displayTitle(
                        selectedParentCategory = selectedParentCategory,
                        selectedCategory = selectedCategory,
                    ),
                    selected = selected,
                    hasChildren = category.children.isNotEmpty(),
                    onClick = {
                        if (category.children.isEmpty()) {
                            expandedParentUrl = null
                            onParentCategorySelected(category)
                        } else {
                            expandedParentUrl = if (expanded) null else category.url
                        }
                    },
                )
                if (category.children.isNotEmpty()) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expandedParentUrl = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = "全部") },
                            onClick = {
                                expandedParentUrl = null
                                onChildCategorySelected(category)
                            },
                        )
                        category.children.forEach { childCategory ->
                            DropdownMenuItem(
                                text = { Text(text = childCategory.title) },
                                onClick = {
                                    expandedParentUrl = null
                                    onChildCategorySelected(childCategory)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendszineCategoryChip(
    text: String,
    selected: Boolean,
    hasChildren: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFFE8F0FE) else Color.White,
        contentColor = if (selected) Color(0xFF1967D2) else Color(0xFF3C4043),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF1967D2) else Color(0xFFE0E3EB),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasChildren) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_chevron_right_24),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f),
                )
            }
        }
    }
}

private fun TrendszineCategory.displayTitle(
    selectedParentCategory: TrendszineCategory,
    selectedCategory: TrendszineCategory,
): String {
    if (url != selectedParentCategory.url) {
        return title
    }
    if (children.isEmpty()) {
        return title
    }
    if (selectedCategory.url == url) {
        return "$title: 全部"
    }
    return "$title: ${selectedCategory.title}"
}
