// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.pullrequest.ui.timeline

import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.comment.CommentInputActionsComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.TimelineComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.update.UiNotifyConnector
import kotlinx.coroutines.flow.MutableStateFlow
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.github.api.data.GHRepositoryPermissionLevel
import org.jetbrains.plugins.github.api.data.GHUser
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort
import org.jetbrains.plugins.github.api.data.pullrequest.timeline.GHPRTimelineItem
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.pullrequest.GHPRTimelineFileEditor
import org.jetbrains.plugins.github.pullrequest.comment.ui.GHCommentTextFieldFactory
import org.jetbrains.plugins.github.pullrequest.comment.ui.GHCommentTextFieldFactory.ActionsConfig
import org.jetbrains.plugins.github.pullrequest.comment.ui.GHCommentTextFieldFactory.AvatarConfig
import org.jetbrains.plugins.github.pullrequest.comment.ui.GHCommentTextFieldModel
import org.jetbrains.plugins.github.pullrequest.comment.ui.submitAction
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRCommentsDataProvider
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRDetailsDataProvider
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRReviewDataProvider
import org.jetbrains.plugins.github.pullrequest.ui.GHApiLoadingErrorHandler
import org.jetbrains.plugins.github.pullrequest.ui.changes.GHPRSuggestedChangeHelper
import org.jetbrains.plugins.github.pullrequest.ui.timeline.GHPRTimelineItemUIUtil.TIMELINE_ITEM_WIDTH
import org.jetbrains.plugins.github.ui.avatars.GHAvatarIconsProvider
import org.jetbrains.plugins.github.ui.component.GHHandledErrorPanelModel
import org.jetbrains.plugins.github.ui.component.GHHtmlErrorPanel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

internal class GHPRFileEditorComponentFactory(private val project: Project,
                                              private val editor: GHPRTimelineFileEditor,
                                              currentDetails: GHPullRequestShort) {

  private val uiDisposable = Disposer.newDisposable().also {
    Disposer.register(editor, it)
  }

  private val detailsModel = SingleValueModel(currentDetails)

  private val errorModel = GHHandledErrorPanelModel(GithubBundle.message("pull.request.timeline.cannot.load"),
                                                    GHApiLoadingErrorHandler(project,
                                                                             editor.securityService.account,
                                                                             editor.timelineLoader::reset))
  private val timelineModel = GHPRTimelineMergingModel()
  private val reviewThreadsModelsProvider = GHPRReviewsThreadsModelsProviderImpl(editor.reviewData, uiDisposable)

  init {
    editor.detailsData.loadDetails(uiDisposable) {
      it.handleOnEdt(uiDisposable) { pr, _ ->
        if (pr != null) detailsModel.value = pr
      }
    }

    editor.timelineLoader.addErrorChangeListener(uiDisposable) {
      errorModel.error = editor.timelineLoader.error
    }
    errorModel.error = editor.timelineLoader.error

    editor.timelineLoader.addDataListener(uiDisposable, object : GHListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) {
        val loadedData = editor.timelineLoader.loadedData
        timelineModel.add(loadedData.subList(startIdx, loadedData.size))
      }

      override fun onDataUpdated(idx: Int) {
        val loadedData = editor.timelineLoader.loadedData
        val item = loadedData[idx]
        timelineModel.update(item)
      }

      override fun onDataRemoved(data: Any) {
        if (data !is GHPRTimelineItem) return
        timelineModel.remove(data)
      }

      override fun onAllDataRemoved() = timelineModel.removeAll()
    })
    timelineModel.add(editor.timelineLoader.loadedData)
  }

  fun create(): JComponent {
    val mainPanel = Wrapper()
    DataManager.registerDataProvider(mainPanel, DataProvider {
      if (PlatformDataKeys.UI_DISPOSABLE.`is`(it)) uiDisposable else null
    })

    val header = GHPRTitleComponent.create(detailsModel).apply {
      border = JBUI.Borders.empty(CodeReviewTimelineUIUtil.HEADER_VERT_PADDING, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
    }

    val suggestedChangesHelper = GHPRSuggestedChangeHelper(project,
                                                           uiDisposable,
                                                           editor.repositoryDataService.remoteCoordinates.repository,
                                                           editor.reviewData,
                                                           editor.detailsData)

    val itemComponentFactory = createItemComponentFactory(
      project,
      editor.detailsData, editor.commentsData, editor.reviewData,
      reviewThreadsModelsProvider, editor.avatarIconsProvider,
      suggestedChangesHelper,
      editor.securityService.ghostUser,
      editor.securityService.currentUser
    )
    val descriptionWrapper = Wrapper().apply {
      isOpaque = false
    }
    detailsModel.addAndInvokeListener {
      descriptionWrapper.setContent(itemComponentFactory.createComponent(detailsModel.value))
    }

    val timeline = TimelineComponentFactory.create(timelineModel, itemComponentFactory, 0)

    val errorPanel = GHHtmlErrorPanel.create(errorModel).apply {
      border = JBUI.Borders.empty(8, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
    }

    val timelineLoader = editor.timelineLoader
    val loadingIcon = JLabel(AnimatedIcon.Default()).apply {
      border = JBUI.Borders.empty(8, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
      isVisible = timelineLoader.loading
    }
    timelineLoader.addLoadingStateChangeListener(uiDisposable) {
      loadingIcon.isVisible = timelineLoader.loading
    }

    val timelinePanel = ScrollablePanel().apply {
      isOpaque = false
      border = JBUI.Borders.empty(CodeReviewTimelineUIUtil.VERT_PADDING, 0)

      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fill()
                           .flowY(),
                         AC().grow().gap("push"))

      add(header, CC().growX().maxWidth("$TIMELINE_ITEM_WIDTH"))
      add(descriptionWrapper, CC().growX())
      add(timeline, CC().growX().minWidth(""))

      add(errorPanel, CC().hideMode(2).width("$TIMELINE_ITEM_WIDTH"))
      add(loadingIcon, CC().hideMode(2).width("$TIMELINE_ITEM_WIDTH"))

      if (editor.securityService.currentUserHasPermissionLevel(GHRepositoryPermissionLevel.READ)) {
        val commentField = createCommentField(editor.commentsData,
                                              editor.avatarIconsProvider,
                                              editor.securityService.currentUser).apply {
          border = JBUI.Borders.empty(CodeReviewChatItemUIUtil.ComponentType.FULL.inputPaddingInsets)
        }
        add(commentField, CC().growX().pushX())
      }
    }

    val scrollPane = ScrollPaneFactory.createScrollPane(timelinePanel, true).apply {
      isOpaque = false
      viewport.isOpaque = false
      verticalScrollBar.model.addChangeListener(object : ChangeListener {
        private var firstScroll = true

        override fun stateChanged(e: ChangeEvent) {
          if (firstScroll && verticalScrollBar.value > 0) firstScroll = false
          if (!firstScroll) {
            if (timelineLoader.canLoadMore()) {
              timelineLoader.loadMore()
            }
          }
        }
      })
    }
    UiNotifyConnector.doWhenFirstShown(scrollPane) {
      timelineLoader.loadMore()
    }

    timelineLoader.addDataListener(uiDisposable, object : GHListLoader.ListDataListener {
      override fun onAllDataRemoved() {
        if (scrollPane.isShowing) timelineLoader.loadMore()
      }
    })

    mainPanel.setContent(scrollPane)

    val actionManager = ActionManager.getInstance()
    actionManager.getAction("Github.PullRequest.Timeline.Update").registerCustomShortcutSet(scrollPane, uiDisposable)
    val groupId = "Github.PullRequest.Timeline.Popup"
    PopupHandler.installPopupMenu(scrollPane, groupId, groupId)

    return mainPanel
  }

  private fun createCommentField(commentService: GHPRCommentsDataProvider,
                                 avatarIconsProvider: GHAvatarIconsProvider,
                                 currentUser: GHUser): JComponent {
    val model = GHCommentTextFieldModel(project) {
      commentService.addComment(EmptyProgressIndicator(), it)
    }

    val submitShortcutText = KeymapUtil.getFirstKeyboardShortcutText(CommentInputComponentFactory.defaultSubmitShortcut)
    val newLineShortcutText = KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.ENTER)

    val actions = ActionsConfig(CommentInputActionsComponentFactory.Config(
      primaryAction = MutableStateFlow(model.submitAction(GithubBundle.message("action.comment.text"))),
      hintInfo = MutableStateFlow(CommentInputActionsComponentFactory.HintInfo(
        submitHint = GithubBundle.message("pull.request.comment.hint", submitShortcutText),
        newLineHint = GithubBundle.message("pull.request.new.line.hint", newLineShortcutText)
      ))
    ))
    val avatarConfig = AvatarConfig(avatarIconsProvider, currentUser, CodeReviewChatItemUIUtil.ComponentType.FULL)
    return GHCommentTextFieldFactory(model).create(actions, avatarConfig)
  }

  private fun createItemComponentFactory(project: Project,
                                         detailsDataProvider: GHPRDetailsDataProvider,
                                         commentsDataProvider: GHPRCommentsDataProvider,
                                         reviewDataProvider: GHPRReviewDataProvider,
                                         reviewThreadsModelsProvider: GHPRReviewsThreadsModelsProvider,
                                         avatarIconsProvider: GHAvatarIconsProvider,
                                         suggestedChangeHelper: GHPRSuggestedChangeHelper,
                                         ghostUser: GHUser,
                                         currentUser: GHUser)
    : GHPRTimelineItemComponentFactory {

    val selectInToolWindowHelper = GHPRSelectInToolWindowHelper(project, detailsModel.value)
    val diffFactory = GHPRReviewThreadDiffComponentFactory(project, EditorFactory.getInstance())
    return GHPRTimelineItemComponentFactory(
      project,
      detailsDataProvider,
      commentsDataProvider,
      reviewDataProvider,
      avatarIconsProvider,
      reviewThreadsModelsProvider,
      diffFactory,
      selectInToolWindowHelper,
      suggestedChangeHelper,
      ghostUser,
      detailsModel.value.author,
      currentUser
    )
  }
}