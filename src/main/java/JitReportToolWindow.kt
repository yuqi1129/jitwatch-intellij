package ru.yole.jitwatch

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

class JitReportToolWindow(val project: Project) : JPanel(BorderLayout()) {
    private val modelService = JitWatchModelService.getInstance(project)
    private val reportTable = TableView<InlineFailureInfo>()
    private val reportTableModel = ListTableModel<InlineFailureInfo>(
            CallSiteColumnInfo, CalleeColumnInfo, CalleeSizeColumnInfo, ReasonColumnInfo)

    init {
        reportTable.setModelAndUpdateColumns(reportTableModel)
        add(JBScrollPane(reportTable), BorderLayout.CENTER)

        updateData()

        reportTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    navigateToSelectedCall()
                }
            }
        })

        modelService.addUpdateListener { updateData() }
    }

    private fun updateData() {
        reportTableModel.items = modelService.inlineFailures
    }

    private fun navigateToSelectedCall() {
        val failureInfo = reportTable.selectedObject ?: return
        val psiMethod = JitWatchModelService.getInstance(project).getPsiMember(failureInfo.callSite) ?: return
        val memberBC = failureInfo.callSite.memberBytecode
        if (memberBC != null) {
            val sourceLine = memberBC.lineTable.findSourceLineForBytecodeOffset(failureInfo.bci)
            if (sourceLine != -1) {
                OpenFileDescriptor(project, psiMethod.containingFile.virtualFile, sourceLine - 1, 0).navigate(true)
                return
            }
        }
        (psiMethod as? NavigatablePsiElement)?.navigate(true)
    }
}

object CallSiteColumnInfo : ColumnInfo<InlineFailureInfo, String>("Call site") {
    override fun valueOf(item: InlineFailureInfo): String? {
        return item.callSite.fullyQualifiedMemberName
    }
}

object CalleeColumnInfo : ColumnInfo<InlineFailureInfo, String>("Callee") {
    override fun valueOf(item: InlineFailureInfo): String? {
        return item.callee.fullyQualifiedMemberName
    }
}

object CalleeSizeColumnInfo : ColumnInfo<InlineFailureInfo, Int>("Callee Size") {
    override fun valueOf(item: InlineFailureInfo): Int? {
        return item.calleeSize
    }
}

object ReasonColumnInfo : ColumnInfo<InlineFailureInfo, String>("Reason") {
    override fun valueOf(item: InlineFailureInfo): String? {
        return item.reason
    }
}
