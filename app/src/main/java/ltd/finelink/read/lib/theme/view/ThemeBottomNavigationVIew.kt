package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import ltd.finelink.read.databinding.ViewNavigationBadgeBinding
import ltd.finelink.read.lib.theme.Selector
import ltd.finelink.read.lib.theme.ThemeStore
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getSecondaryTextColor
import ltd.finelink.read.ui.widget.text.BadgeView
import ltd.finelink.read.utils.ColorUtils

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val bgColor = context.bottomBackground
        setBackgroundColor(bgColor)
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context)).create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
    }

    fun addBadgeView(index: Int): BadgeView {
        //获取底部菜单view
        val menuView = getChildAt(0) as BottomNavigationMenuView
        //获取第index个itemView
        val itemView = menuView.getChildAt(index) as BottomNavigationItemView
        val badgeBinding = ViewNavigationBadgeBinding.inflate(LayoutInflater.from(context))
        itemView.addView(badgeBinding.root)
        return badgeBinding.viewBadge
    }

}