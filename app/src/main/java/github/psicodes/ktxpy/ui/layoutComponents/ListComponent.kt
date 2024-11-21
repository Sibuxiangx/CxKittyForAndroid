package github.psicodes.ktxpy.ui.layoutComponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.terminal.TerminalSession
import github.psicodes.ktxpy.R
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ListComponent(
    modifier: Modifier = Modifier,
    session : TerminalSession,
    icon : Int = R.drawable.python_icon
){
    Row (
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .size(45.dp)
                .padding(5.dp),
        )
        Column{
            Text(
                "Name : ${session.mSessionName}",
                fontFamily = FontFamily(Font(resId = R.font.custom_sans)),
                modifier = Modifier.padding(top=2.dp),
                maxLines = 1,
                fontSize = 12.sp
            )
            Text(
                text = "Handle : ${session.mHandle}",
                maxLines = 1,
                fontFamily = FontFamily(Font(resId = R.font.custom_sans)),
                fontSize = 10.sp,
            )
            Text(
                text = "Pid : ${session.pid}",
                maxLines = 1,
                fontFamily = FontFamily(Font(resId = R.font.custom_sans)),
                fontSize = 10.sp,
            )
        }
    }
}