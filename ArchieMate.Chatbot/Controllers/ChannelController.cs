using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Models.DTOs.Incoming;
using ArchieMate.Chatbot.Models.DTOs.Outgoing;
using ArchieMate.Chatbot.Services.Database.Repositories;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/channel")]
public class ChannelController : ControllerBase
{
    private readonly ILogger<ChannelController> logger;
    private readonly IChannelsRepository channelsRepository;

    public ChannelController(ILogger<ChannelController> logger, IChannelsRepository channelsRepository)
    {
        this.logger = logger;
        this.channelsRepository = channelsRepository;
    }

    [HttpGet]
    public async Task<IActionResult> GetAllAsync()
    {
        var channels = await this.channelsRepository.GetAllAsync();

        return Ok(channels);
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetAsync(Guid id)
    {
        var channel = await this.channelsRepository.GetAsync(id);

        return GetChannelResponse(channel);
    }

    [HttpGet("id/{id}")]
    public async Task<IActionResult> GetByRoomIdAsync(int roomId)
    {
        var channel = await this.channelsRepository.GetByRoomIdAsync(roomId);

        return GetChannelResponse(channel);
    }

    [HttpGet("name/{name}")]
    public async Task<IActionResult> GetByNameAsync(string name)
    {
        var channel = await this.channelsRepository.GetByNameAsync(name);

        return GetChannelResponse(channel);
    }

    [HttpPost]
    public async Task<IActionResult> AddAsync(CreateChannelDTO channelDTO)
    {
        var channelModel = channelDTO.ToModel();

        await this.channelsRepository.AddAsync(channelModel);

        return CreatedAtAction(nameof(GetAsync), new { Id = channelModel.Id }, channelModel.ToDTO());
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateAsync(Guid id, UpdateChannelDTO channelDTO)
    {
        var channel = await this.channelsRepository.GetAsync(id);

        if (channel is null)
        {
            return NotFound();
        }

        var updatedChannel = new Channel
        {
            Id = channel.Id,
            Name = channelDTO.Name,
            RoomId = channelDTO.RoomId,
            Join = channelDTO.Join
        };

        await this.channelsRepository.UpdateAsync(updatedChannel);

        return NoContent();
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteAsync(Guid id)
    {
        var channel = await this.channelsRepository.GetAsync(id);

        if (channel is null)
        {
            return NotFound();
        }

        await this.channelsRepository.DeleteAsync(channel);

        return NoContent();
    }

    private IActionResult GetChannelResponse(Channel? channel)
    {
        if (channel is null)
        {
            return NotFound();
        }

        return Ok(channel.ToDTO());
    }
}
