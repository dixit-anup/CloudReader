package mdettlaff.cloudreader.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import mdettlaff.cloudreader.domain.Feed;
import mdettlaff.cloudreader.domain.FeedItem;
import mdettlaff.cloudreader.test.AbstractPersistenceTestContext;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class FeedItemDaoTest extends AbstractPersistenceTestContext {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private FeedItemDao dao;

	@Test
	@Transactional
	public void testFindFeeds() {
		// exercise
		List<Feed> results = dao.findFeeds();
		// verify
		assertEquals(4, results.size());
		assertEquals("http://queencorner.ovh.org/rss.xml", results.get(0).getUrl());
		assertTrue(results.get(0).getItems().isEmpty());
		assertEquals("https://github.com/mdettlaff-mb/CloudReader/commits/master.atom", results.get(1).getUrl());
		assertTrue(results.get(1).getItems().isEmpty());
	}

	@Test
	@Transactional
	public void testFind() {
		// exercise
		List<FeedItem> results = dao.find(false, 3, Arrays.asList("item-0002", "item-0004"));
		// verify
		assertEquals(3, results.size());
		FeedItem result1 = results.get(0);
		assertEquals("item-0001", result1.getGuid());
		assertEquals(false, result1.isRead());
		assertEquals("My item 1", result1.getTitle());
		assertEquals("url1", result1.getFeed().getUrl());
		assertEquals("My feed 1", result1.getFeed().getTitle());
		assertEquals("item-0003", results.get(1).getGuid());
		assertEquals("item-0005", results.get(2).getGuid());
	}

	@Test
	@Transactional
	public void testCount() {
		// exercise
		long result = dao.count(false);
		// verify
		assertEquals(6, result);
	}

	@Test
	@Transactional
	public void testSaveFeed_Create() {
		// prepare data
		Feed feed = new Feed("savedurl");
		feed.setTitle("My saved feed");
		List<FeedItem> items = new ArrayList<>();
		items.add(prepareItem("item-a001", "My saved item 1", feed));
		items.add(prepareItem("item-a002", "My saved item 2", feed));
		feed.setItems(items);
		// exercise
		long result = dao.saveFeed(feed);
		// verify
		assertEquals(2, result);
		Feed newFeed = em.find(Feed.class, "savedurl");
		assertNotNull("feed was not saved successfully", newFeed);
		assertEquals("savedurl", newFeed.getUrl());
		assertEquals("My saved feed", newFeed.getTitle());
		List<FeedItem> newItems = newFeed.getItems();
		assertEquals(2, newItems.size());
		assertSame(newFeed, newItems.get(0).getFeed());
		assertEquals("item-a001", newItems.get(0).getGuid());
		assertEquals("My saved item 1", newItems.get(0).getTitle());
		assertSame(newFeed, newItems.get(1).getFeed());
		assertEquals("item-a002", newItems.get(1).getGuid());
	}

	@Test
	@Transactional
	public void testSaveFeed_Update() {
		// prepare data
		Feed feed = new Feed("url1");
		feed.setTitle("My updated feed");
		feed.setLink("My updated link");
		List<FeedItem> items = new ArrayList<>();
		items.add(prepareItem("item-b001", "My added item 1", feed));
		feed.setItems(items);
		// exercise
		long result = dao.saveFeed(feed);
		// verify
		assertEquals(1, result);
		Feed updatedFeed = em.find(Feed.class, "url1");
		assertEquals("url1", updatedFeed.getUrl());
		assertEquals("My updated feed", updatedFeed.getTitle());
		assertEquals("My updated link", updatedFeed.getLink());
		List<FeedItem> newItems = updatedFeed.getItems();
		assertEquals(3, newItems.size());
		assertSame(updatedFeed, newItems.get(2).getFeed());
		assertEquals("item-b001", newItems.get(2).getGuid());
		assertEquals("My added item 1", newItems.get(2).getTitle());
	}

	private FeedItem prepareItem(String guid, String title, Feed feed) {
		FeedItem item = new FeedItem();
		item.setGuid(guid);
		item.setTitle(title);
		item.setFeed(feed);
		return item;
	}

	@Test
	@Transactional
	public void shouldThrowExceptionForDuplicateFeedUrl() {
		// prepare data
		Feed feed = new Feed("url1");
		// exercise
		try {
			em.persist(feed);
			em.flush();
			fail();
		} catch (PersistenceException e) {
			assertTrue(e.getCause() instanceof ConstraintViolationException);
		}
	}
}