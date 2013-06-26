package org.springframework.site.blog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.site.blog.web.BlogPostsPageRequest;
import org.springframework.site.blog.web.NoSuchBlogPostException;
import org.springframework.site.services.MarkdownService;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlogService_QueryTests {
	private BlogService service;

	@Mock
	private PostRepository postRepository;

	@Mock
	private MarkdownService markdownService;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void setup() {
		service = new BlogService(postRepository, markdownService);
	}

	@Test
	public void postIsRetrievable() {
		Post post = PostBuilder.post().build();
		when(postRepository.findOne(anyLong())).thenReturn(post);
		assertThat(post, equalTo(service.getPost(post.getId())));
		verify(postRepository).findOne(anyLong());
	}

	@Test
	public void publishedPostIsRetrievable() {
		Post post = PostBuilder.post().build();
		when(postRepository.findByIdAndDraftFalse(anyLong())).thenReturn(post);
		assertThat(post, equalTo(service.getPublishedPost(post.getId())));
		verify(postRepository).findByIdAndDraftFalse(anyLong());
	}

	@Test
	public void nonExistentPost() {
		when(postRepository.findOne(anyLong())).thenReturn(null);
		expected.expect(NoSuchBlogPostException.class);
		service.getPost(999L);
	}

	@Test
	public void nonPublishedPost() {
		when(postRepository.findByIdAndDraftFalse(anyLong())).thenReturn(null);
		expected.expect(NoSuchBlogPostException.class);
		service.getPost(999L);
	}

	@Test
	public void listPosts() {
		Pageable firstTenPosts = new BlogPostsPageRequest(1);
		List<Post> posts = new ArrayList<Post>();
		posts.add(PostBuilder.post().build());
		Page<Post> page = new PageImpl<Post>(posts);

		when(postRepository.findByDraftFalse(firstTenPosts)).thenReturn(page);

		assertThat(service.mostRecentPosts(firstTenPosts), is(posts));
	}

	@Test
	public void listPostsForCategory() {
		Pageable firstTenPosts = new BlogPostsPageRequest(1);
		List<Post> posts = new ArrayList<Post>();
		posts.add(PostBuilder.post().category(PostCategory.ENGINEERING).build());
		Page<Post> page = new PageImpl<Post>(posts);

		when(postRepository.findByCategoryAndDraftFalse(PostCategory.ENGINEERING, firstTenPosts)).thenReturn(page);

		assertThat(service.mostRecentPosts(PostCategory.ENGINEERING, firstTenPosts), is(posts));
	}

	@Test
	public void givenOnePage_paginationInfoBasedOnCurrentPageAndTotalPosts() {
		when(postRepository.count()).thenReturn(1L);
		PaginationInfo paginationInfo = service.paginationInfo(new PageRequest(0, 10));
		assertThat(paginationInfo.getCurrentPage(), is(equalTo(1L)));
		assertThat(paginationInfo.getTotalPages(), is(equalTo(1L)));
	}

	@Test
	public void givenManyPages_paginationInfoBasedOnCurrentPageAndTotalPosts() {
		when(postRepository.count()).thenReturn(101L);
		PaginationInfo paginationInfo = service.paginationInfo(new PageRequest(0, 10));
		assertThat(paginationInfo.getCurrentPage(), is(equalTo(1L)));
		assertThat(paginationInfo.getTotalPages(), is(equalTo(11L)));
	}

	@Test
	public void extractFirstParagraph() {
		assertEquals("xx", service.extractFirstParagraph("xxxxx", 2));
		assertEquals("xx", service.extractFirstParagraph("xx\n\nxxx", 20));
		assertEquals("xx", service.extractFirstParagraph("xx xx\n\nxxx", 4));
	}

	@Test
	public void listBroadcasts() {
		Pageable firstTenPosts = new BlogPostsPageRequest(1);
		List<Post> posts = new ArrayList<Post>();
		posts.add(PostBuilder.post().isBroadcast().build());
		Page<Post> page = new PageImpl<Post>(posts);

		when(postRepository.findByBroadcastAndDraftFalse(eq(true), any(Pageable.class))).thenReturn(page);

		assertThat(service.mostRecentBroadcastPosts(firstTenPosts), is(posts));
	}

	@Test
	public void allPosts() {
		List<Post> posts = new ArrayList<Post>();
		posts.add(PostBuilder.post().draft().build());
		posts.add(PostBuilder.post().build());
		Page<Post> page = new PageImpl<Post>(posts);

		when(postRepository.findAll(any(Pageable.class))).thenReturn(page);

		assertThat(service.allPosts(new BlogPostsPageRequest(1)), is(posts));
	}
}
