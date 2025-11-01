package com.progresstracker.bodylens.navigation

/**
 * Navigation routes for the app
 */
object Routes {
    const val PIN_SETUP = "pin_setup"
    const val PIN_ENTRY = "pin_entry"
    const val HOME = "home"
    const val BODY_PARTS = "body_parts"
    const val ADD_BODY_PART = "add_body_part"
    const val PHOTO_SESSION = "photo_session"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    const val PHOTO_VIEWER = "photo_viewer/{sessionId}/{photoIndex}"
    const val COMPARISON_SELECTION = "comparison_selection"
    const val COMPARISON_RESULT = "comparison_result"
    const val PHOTO_IMPORT = "photo_import"

    fun sessionDetail(sessionId: Long) = "session_detail/$sessionId"
    fun photoViewer(sessionId: Long, photoIndex: Int) = "photo_viewer/$sessionId/$photoIndex"
}
