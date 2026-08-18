package com.jntuh.rest;


import com.jntuh.response.AppDetailRP;
import com.jntuh.response.AuthorDetailRP;
import com.jntuh.response.AuthorRP;
import com.jntuh.response.BookDetailRP;
import com.jntuh.response.BraintreeCheckOutRP;
import com.jntuh.response.CatRP;
import com.jntuh.response.DeleteAccRP;
import com.jntuh.response.EditProfileRP;
import com.jntuh.response.FavoriteRP;
import com.jntuh.response.HomeRP;
import com.jntuh.response.LoginRP;
import com.jntuh.response.PayUMoneyHashRP;
import com.jntuh.response.PaymentSuccessRP;
import com.jntuh.response.PaypalTokenRP;
import com.jntuh.response.PlanRP;
import com.jntuh.response.PostRateRP;
import com.jntuh.response.RateReviewRP;
import com.jntuh.response.RazorPayTokenRP;
import com.jntuh.response.RegisterRP;
import com.jntuh.response.StripeCheckOutRP;
import com.jntuh.response.SubCatListBookRP;
import com.jntuh.response.SubCatRP;
import com.jntuh.response.UniversityRP;
import com.jntuh.response.DepartmentRP;
import com.jntuh.response.CollegeRP;
import com.jntuh.response.UserUploadRP;
import com.jntuh.response.MyUploadRP;
import com.jntuh.response.MediaFeedRP;import com.jntuh.response.MyMediaRP;
import com.jntuh.response.MediaUploadRP;
import com.jntuh.response.MediaLikeRP;
import com.jntuh.response.MediaCommentRP;
import com.jntuh.response.MediaCommentListRP;
import com.jntuh.response.MediaNotificationListRP;
import com.jntuh.response.ResultRP;
import com.jntuh.response.ResultSaveRP;
import com.jntuh.response.ReportCardRP;
import com.jntuh.response.ShopCategoryRP;
import com.jntuh.response.ShopProductRP;
import com.jntuh.response.ShopLinksRP;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiInterface {

    //get home data
    @POST("home")
    @FormUrlEncoded
    Call<HomeRP> getHomeData(@Field("data") String data);

    //get cat data
    @POST("category")
    @FormUrlEncoded
    Call<CatRP> getCatData(@Field("data") String data, @Query("page") int page);

    //get sub cat data
    @POST("subcategory")
    @FormUrlEncoded
    Call<SubCatRP> getSubCatData(@Field("data") String data, @Query("page") int page);

    //get author data
    @POST("authors")
    @FormUrlEncoded
    Call<AuthorRP> getAuthorData(@Field("data") String data, @Query("page") int page);

    //get book by sub cat data
    @POST("books_by_sub_cat")
    @FormUrlEncoded
    Call<SubCatListBookRP> getBookListBySubCatData(@Field("data") String data, @Query("page") int page);

    //get  book by cat data
    @POST("books_by_cat")
    @FormUrlEncoded
    Call<SubCatListBookRP> getBookListByCatData(@Field("data") String data, @Query("page") int page);

    //get  book by author data
    @POST("books_by_author")
    @FormUrlEncoded
    Call<SubCatListBookRP> getBookListByAuthorData(@Field("data") String data, @Query("page") int page);

    //get  app detail
    @POST("app_details")
    @FormUrlEncoded
    Call<AppDetailRP> getAppDetailData(@Field("data") String data);

    //get  app book detail
    @POST("books_details")
    @FormUrlEncoded
    Call<BookDetailRP> getBookDetailData(@Field("data") String data);

    //get  rate review list
    @POST("books_reviews_list")
    @FormUrlEncoded
    Call<RateReviewRP> getRateReviewListData(@Field("data") String data);

    //get  post rate
    @POST("post_rate")
    @FormUrlEncoded
    Call<PostRateRP> getPostRateData(@Field("data") String data);

    //get  post view
    @POST("post_view")
    @FormUrlEncoded
    Call<JsonObject> getPostViewData(@Field("data") String data);

    //get  post report book
    @POST("user_reports")
    @FormUrlEncoded
    Call<PostRateRP> getReportBookData(@Field("data") String data);

    //get  post report comment
    @POST("user_reports")
    @FormUrlEncoded
    Call<PostRateRP> getReportCommentData(@Field("data") String data);

    //get  post delete comment
    @POST("delete_user_review")
    @FormUrlEncoded
    Call<PostRateRP> getDeleteCommentData(@Field("data") String data);

    //get  setting data
    @POST("app_details")
    @FormUrlEncoded
    Call<AppDetailRP> getSettingPageData(@Field("data") String data);

    //get  login
    @POST("login")
    @FormUrlEncoded
    Call<LoginRP> getLoginData(@Field("data") String data);

    //get  login social
    @POST("social_login")
    @FormUrlEncoded
    Call<LoginRP> getLoginSocialData(@Field("data") String data);

    //get  register
    @POST("signup")
    @FormUrlEncoded
    Call<RegisterRP> getRegisterData(@Field("data") String data);

    //get  forgot
    @POST("forgot_password")
    @FormUrlEncoded
    Call<RegisterRP> getForgotData(@Field("data") String data);

    //get  post continue read
    @POST("post_continue_read")
    @FormUrlEncoded
    Call<JsonObject> getBookContinueData(@Field("data") String data);

    //get  profile
    @POST("profile")
    @FormUrlEncoded
    Call<LoginRP> getProfileData(@Field("data") String data);

    //get fav data
    @POST("user_favourite_post_list")
    @FormUrlEncoded
    Call<SubCatListBookRP> getFavData(@Field("data") String data);

    //get continue data
    @POST("continue_read_list")
    @FormUrlEncoded
    Call<SubCatListBookRP> getContinueData(@Field("data") String data);

    //get edit profile data
    @POST("profile_update")
    @Multipart
    Call<EditProfileRP> getEditProfileData(@Part("data") RequestBody data, @Part MultipartBody.Part part);

    //complete-profile (no image upload): saves university/department/college
    @POST("profile_update")
    @FormUrlEncoded
    Call<EditProfileRP> getCompleteProfileData(@Field("data") String data);

    //get book latest data
    @POST("latest_books")
    @FormUrlEncoded
    Call<SubCatListBookRP> getBookLatestData(@Field("data") String data);

    //get trend book data
    @POST("trending_books")
    @FormUrlEncoded
    Call<SubCatListBookRP> getTrendingBookData(@Field("data") String data);

    //get book search data
    @POST("search_book")
    @FormUrlEncoded
    Call<SubCatListBookRP> getSearchBookData(@Field("data") String data, @Query("page") int page);

    //get fav un fav book data
    @POST("post_favourite")
    @FormUrlEncoded
    Call<FavoriteRP> getFavUnFavBookData(@Field("data") String data);

    //get plan list data
    @POST("subscription_plan")
    @FormUrlEncoded
    Call<PlanRP> getPlanListData(@Field("data") String data);

    //get  payment method
    @POST("payment_settings")
    @FormUrlEncoded
    Call<ResponseBody> getPaymentMethodData(@Field("data") String data);

    //get success payment
    @POST("{apiName}")
    @FormUrlEncoded
    Call<PaymentSuccessRP> getPaymentSuccessData(@Path ("apiName")String apiName,@Field("data") String data);

    //get stripe token
    @POST("stripe_token_get")
    @FormUrlEncoded
    Call<StripeCheckOutRP> getStripeTokenData(@Field("data") String data);

    //get paypal token
    @POST("get_braintree_token")
    @FormUrlEncoded
    Call<PaypalTokenRP> getPaypalTokenData(@Field("data") String data);

    //get paypal check out
    @POST("braintree_checkout")
    @FormUrlEncoded
    Call<BraintreeCheckOutRP> getPaypalCheckOutData(@Field("data") String data);


    //get razor pay token
    @POST("razorpay_order_id_get")
    @FormUrlEncoded
    Call<RazorPayTokenRP> getRazorPayTokenData(@Field("data") String data);

    //get payu hash key
    @POST("get_payu_hash")
    @FormUrlEncoded
    Call<PayUMoneyHashRP> getPayUMoneyHashData(@Field("data") String data);

    //get  my subscription
    @POST("check_user_plan")
    @FormUrlEncoded
    Call<ResponseBody> getMySubscriptionData(@Field("data") String data);

    //get home section data
    @POST("home_section")
    @FormUrlEncoded
    Call<HomeRP> getHomeSectionSingleData(@Field("data") String data);

    //get rent list data
    @POST("user_rent_list")
    @FormUrlEncoded
    Call<SubCatListBookRP> getRentBookListData(@Field("data") String data);

    //get filter search data
    @POST("filter_book")
    @FormUrlEncoded
    Call<SubCatListBookRP> getFilterSearchBookData(@Field("data") String data, @Query("page") int page);

    //get all author data
    @POST("all_authors")
    @FormUrlEncoded
    Call<AuthorRP> getAllAuthorData(@Field("data") String data);

    //get all cat data
    @POST("all_category")
    @FormUrlEncoded
    Call<CatRP> getAllCatData(@Field("data") String data);

    //get  app author detail
    @POST("author_info")
    @FormUrlEncoded
    Call<AuthorDetailRP> getAuthorDetailData(@Field("data") String data);

    //get  post download
    @POST("post_download")
    @FormUrlEncoded
    Call<JsonObject> getPostDownloadData(@Field("data") String data);

    //get  delete data
    @POST("account_delete")
    @FormUrlEncoded
    Call<DeleteAccRP> getDeleteData(@Field("data") String data);

    //get university list (registration dropdown)
    @POST("university_list")
    @FormUrlEncoded
    Call<UniversityRP> getUniversityListData(@Field("data") String data);

    //get department list (optional university_id filter for cascade)
    @POST("department_list")
    @FormUrlEncoded
    Call<DepartmentRP> getDepartmentListData(@Field("data") String data);

    //get college list (registration dropdown)
    @POST("college_list")
    @FormUrlEncoded
    Call<CollegeRP> getCollegeListData(@Field("data") String data);

    //user uploads a book (multipart: data + optional cover + optional file)
    @POST("user_upload_book")
    @Multipart
    Call<UserUploadRP> getUserUploadBookData(@Part("data") RequestBody data,
                                             @Part MultipartBody.Part bookImage,
                                             @Part MultipartBody.Part bookFile);

    //get user's own uploads with moderation status
    @POST("my_uploaded_books")
    @FormUrlEncoded
    Call<MyUploadRP> getMyUploadedBooksData(@Field("data") String data);

    // --- Media feed (photos + videos) ---

    //scroll feed (approved + live). page for pagination, optional media_type filter.
    @POST("media_feed")
    @FormUrlEncoded
    Call<MediaFeedRP> getMediaFeedData(@Field("data") String data, @Query("page") int page);

    //upload a photo/video (multipart: data + media_file + optional thumb_file)
    @POST("media_upload")
    @Multipart
    Call<MediaUploadRP> getMediaUploadData(@Part("data") RequestBody data,
                                           @Part MultipartBody.Part mediaFile,
                                           @Part MultipartBody.Part thumbFile);

    /*
     * Same endpoint, but the caller supplies the file parts itself so a post can
     * carry several photos: the first is sent as "media_file" (what the server
     * reads today) and any extras as "media_file[]".
     */
    @POST("media_upload")
    @Multipart
    Call<MediaUploadRP> getMediaUploadMultiData(@Part("data") RequestBody data,
                                                @Part List<MultipartBody.Part> files);

    //the user's own media posts with moderation status
    @POST("my_uploaded_media")
    @FormUrlEncoded
    Call<MyMediaRP> getMyUploadedMediaData(@Field("data") String data);

    //delete own post
    @POST("media_delete")
    @FormUrlEncoded
    Call<PostRateRP> getMediaDeleteData(@Field("data") String data);

    //increment view count as a post scrolls into view
    @POST("media_view")
    @FormUrlEncoded
    Call<JsonObject> getMediaViewData(@Field("data") String data);

    // --- Likes ---
    @POST("media_like")
    @FormUrlEncoded
    Call<MediaLikeRP> getMediaLikeData(@Field("data") String data);

    // --- Comments ---
    @POST("media_comment_add")
    @FormUrlEncoded
    Call<MediaCommentRP> getMediaCommentAddData(@Field("data") String data);

    @POST("media_comment_list")
    @FormUrlEncoded
    Call<MediaCommentListRP> getMediaCommentListData(@Field("data") String data);

    @POST("media_comment_delete")
    @FormUrlEncoded
    Call<MediaCommentRP> getMediaCommentDeleteData(@Field("data") String data);

    // --- Notifications ---
    @POST("media_notification_list")
    @FormUrlEncoded
    Call<MediaNotificationListRP> getMediaNotificationListData(@Field("data") String data);

    @POST("media_notification_read")
    @FormUrlEncoded
    Call<PostRateRP> getMediaNotificationReadData(@Field("data") String data);

    // --- Results (My Results feature) ---
    @POST("result_get")
    @FormUrlEncoded
    Call<ResultRP> getResult(@Field("data") String data);

    @POST("result_save")
    @FormUrlEncoded
    Call<ResultSaveRP> saveResult(@Field("data") String data);

    /**
     * Pull the student's mark sheet from the university feed by hall ticket.
     * Answers state = ready | queued | error; "queued" means the upstream is
     * still scraping, so the caller polls rather than failing.
     */
    @POST("result_fetch")
    @FormUrlEncoded
    Call<ResultSaveRP> fetchResult(@Field("data") String data);

    /**
     * Who a hall ticket belongs to. Used during sign-up, before an account
     * exists, to fill in name / father / branch / regulation.
     */
    @POST("hall_ticket_lookup")
    @FormUrlEncoded
    Call<com.jntuh.response.HallTicketRP> lookupHallTicket(@Field("data") String data);

    @POST("report_generate")
    @FormUrlEncoded
    Call<ReportCardRP> generateReportCard(@Field("data") String data);

    // ---- Shop (MadeForU WooCommerce proxy) ----
    @POST("shop_categories")
    @FormUrlEncoded
    Call<ShopCategoryRP> getShopCategories(@Field("data") String data);

    @POST("shop_products")
    @FormUrlEncoded
    Call<ShopProductRP> getShopProducts(@Field("data") String data);

    @POST("shop_product_detail")
    @FormUrlEncoded
    Call<ShopProductRP> getShopProductDetail(@Field("data") String data);

    @POST("shop_links")
    @FormUrlEncoded
    Call<ShopLinksRP> getShopLinks(@Field("data") String data);
}
