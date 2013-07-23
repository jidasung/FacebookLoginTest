package com.jidasung.facebooklogintest;

/*
 * 2013. 07. 23.
 * 
 * Facebook Login Test
 * 
 * Hyeong-Gyu Jeong
 * 	http://jidasung.com
 * 	hunguenglish@gmail.com
 * 
 * 
 * REF : Facebook Android SDK / SessionLoginSample
 * 
 */


import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;


public class MainActivity extends Activity {

    private static final String URL_PREFIX_FRIENDS = "https://graph.facebook.com/me/friends?access_token=";

	private TextView textInstructionsOrLink;
	private TextView profileName;
	private ImageView profileImage;
    private LoginButton buttonLoginLogout;
    private Session.StatusCallback statusCallback = new SessionStatusCallback();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // findViewById
        buttonLoginLogout = (LoginButton)findViewById(R.id.login);
        textInstructionsOrLink = (TextView)findViewById(R.id.textview1);
        profileName = (TextView)findViewById(R.id.textView2);
        profileImage = (ImageView)findViewById(R.id.imageView1);

        // ACCESS TOKEN 포함하여 로깅
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        // session 설정
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, statusCallback, savedInstanceState); // Shared Preference 에 저장
            }
            if (session == null) {
                session = new Session(this);
            }
            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {	// 토큰이 로드된 상태라면
                session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback)
                												.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO)); // 콜백함수 설정할 때 SSO 진입을 막는다.
            }
        }

        updateView();	// 뷰 업데이트 (프로필 정보 가져오기)
    }

    @Override
    public void onStart() {
        super.onStart();
        Session.getActiveSession().addCallback(statusCallback);	// 현재 세션에 콜백함수 추가.
    }

    @Override
    public void onStop() {
        super.onStop();
        Session.getActiveSession().removeCallback(statusCallback);	// 현재 세션에 콜백함수 제거. 
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);	// 현재 세션의 onActivityResult 함수.
        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);	// 현재 세션을 액티비티의 상태로 저장.
    }
    
    
    // 뷰가 업데이트 될 때 하는 일 - 로그인 후 프로필 정보를 가져온다.
    private void updateView() {
        Session session = Session.getActiveSession();
        if (session.isOpened()) {

            textInstructionsOrLink.setText(URL_PREFIX_FRIENDS + session.getAccessToken()); // access token 으로 graph를 이용해 친구정보를 가져온다.
            textInstructionsOrLink.setLinksClickable(true);
            buttonLoginLogout.setUserInfoChangedCallback(new UserInfoChangedCallback() {	// 유저 정보가 바뀐 경우 콜백함수 설정 (로그인 버튼)
				
				@Override
				public void onUserInfoFetched(GraphUser user) {
					// TODO Auto-generated method stub
					if(user != null)	// 유저가 있으면
					{
						String url = "https://graph.facebook.com/" + user.getId() + "/picture?type=large";	// 역시 graph 를 통해 프로필 사진을 가져온다.
						ConnectImage mConnectImage = new ConnectImage();	// input stream 에 사진을 받아오는 asynctask.
						mConnectImage.execute(url);	// url -> drawable
						Drawable drawable = null;
						try {
							drawable = mConnectImage.get(); // drawable 을 받아온다.
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						profileName.setText("Hello, " + user.getName());	// 유저 이름을 넣고
						profileImage.setImageDrawable(drawable);			// 사진을 넣는다.
					}
				}
			});
            // 로그인이 되었으므로 버튼에 로그아웃 리스너를 넣는다.
            buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) { onClickLogout(); }
            });
        } else {
        	// 기본 default text 와 image 를 넣는다.
            textInstructionsOrLink.setText("Link");
            profileName.setText("No User");
            profileImage.setImageResource(R.drawable.com_facebook_profile_picture_blank_portrait);
            // 로그아웃 되었으므로 버튼에 로그인 리스너를 넣는다.
            buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) { onClickLogin(); }
            });
        }
    }

    private void onClickLogin() {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
        	// 세션이 열리거나 닫히 않았으면 새로운 세션을 열 때 SSO 진입을 막는 setLoginBehavior 를 사용한다.
            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback).setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO));
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
    }

    private void onClickLogout() {
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
        	// 세션 정보 삭제
            session.closeAndClearTokenInformation();
        }
    }

    
    // Session 의 StatusCallback 함수를 오버라이드해서 updateView() 함수를 호출한다.
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }
	
	
}


// 프로필 사진을 받아오는 AsyncTask. url 을 받아와 drawable 를 반환한다.
class ConnectImage extends AsyncTask<String, Integer, Drawable> {

	@Override
	protected Drawable doInBackground(String... params) {
		// TODO Auto-generated method stub
		
		String url = params[0];
		try {
			InputStream is = (InputStream)new URL(url).getContent();
			Drawable d = Drawable.createFromStream(is, "src name");
			return d;
		} catch (Exception e) {
			Log.e("MainActivity",e.toString());
			return null;
		}
	}
	
}