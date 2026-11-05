package dev.drift.intellij.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File

class SeeRepositoryAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = resolveRepoUrl(e.project) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        resolveRepoUrl(e.project)?.let { BrowserUtil.browse(it) }
    }

    override fun getActionUpdateThread() =
        com.intellij.openapi.actionSystem.ActionUpdateThread.BGT

    private fun resolveRepoUrl(project: Project?): String? {
        val basePath = project?.basePath ?: return null
        val gitConfig = File("$basePath/.git/config")
        if (!gitConfig.exists()) return null

        val repoPath = gitConfig.readLines()
            .asSequence()
            .mapNotNull { REMOTE_REGEX.find(it)?.groupValues?.get(1) }
            .firstOrNull() ?: return null

        return "https://github.com/$repoPath"
    }

    companion object {
        // Matches SSH (git@github.com:org/repo.git) and HTTPS (https://github.com/org/repo.git)
        private val REMOTE_REGEX =
            Regex("""github\.com[:/](drift-language/[^.\s\n/]+?)(?:\.git)?\s*$""")
    }
}
